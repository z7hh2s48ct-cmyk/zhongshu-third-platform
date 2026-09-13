package cn.zszj.module.system.dal.redis.sms;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static cn.zszj.module.system.dal.redis.RedisKeyConstants.SMS_CODE_SEND_IP_COUNT;
import static cn.zszj.module.system.dal.redis.RedisKeyConstants.SMS_CODE_VALIDATE_ATTEMPTS;

/**
 * ZS-LOGIN-004：短信验证码「安全计数器」的 RedisDAO
 * <p>
 * 承载两类与登录安全强相关、但<b>不属于业务数据</b>的计数状态：
 * <ol>
 *     <li>验证码校验失败次数（按 手机号 + 场景），用于暴力破解防护；</li>
 *     <li>每 IP 短信发送次数（按 小时 / 天 滚动桶），用于封堵「同一 IP 轮换手机号」的短信喷洒。</li>
 * </ol>
 * <p>
 * 选型说明：刻意存 Redis 而<b>不新增 DB 列</b>，避免 Flyway 迁移与 ZS-DB-019.B 的 PG 回归耦合；
 * 两类计数都需要 TTL 自然过期（锁定期到期自动解锁、时间桶自然归零），Redis 原生支持而 DB 需额外清理任务。
 * <p>
 * 容错策略（fail-closed）：本类<b>不吞 Redis 异常</b>。理由是本底座的访问令牌本身即以 Redis 为一级缓存，
 * Redis 不可用时登录链路已整体不可用，此时「放行」等于把暴力破解防护与频控同时清零，
 * 故宁可让验证码校验随 Redis 一起失败，也不静默降级为「无限制」。
 * <p>
 * ZS-LOGIN-004 codex r0 修复（P2-C/P2-D/P2-E）：所有「检查 + 计数 + 设过期」的复合动作一律合并为
 * <b>单个 Lua 脚本原子执行</b>（{@link DefaultRedisScript}），杜绝以下三类竞态：
 * <ul>
 *     <li>P2-E：{@code INCR} 与 {@code EXPIRE} 分离——首个 INCR 成功但进程/通信在 EXPIRE 前中断 → 计数键永无 TTL，
 *         一旦达上限连正确码都被无限期拒绝；</li>
 *     <li>P2-C：校验尝试「读计数-判上限」与「失败自增」分离——并发下多个猜测都读到低于上限而全部抵达 DB；</li>
 *     <li>P2-D：每 IP 发送配额「校验-建码-自增」分离——并发下多个请求都通过校验、全部建码发短信、超发配额。</li>
 * </ul>
 * <p>
 * ZS-LOGIN-004 codex r1 修复（在 r0 原子预留基础上精修「释放生命周期」与「Redis Cluster 兼容」，不推倒 r0 成果）：
 * <ul>
 *     <li>P1(Cluster)：每 IP 配额脚本一次操作小时桶 + 天桶<b>两个键</b>，原键无 hash tag → Cluster 下哈希到不同槽 →
 *         抛 {@code CROSSSLOT}。改为把 IP 包进 {@code {...}} hash tag（{@link #formatIpSendCountKey}），两桶键同槽，
 *         脚本恢复 Cluster 安全。校验尝试相关脚本经 r1 改造后为<b>单键</b>（Hash），本身即 Cluster 安全，无需 hash tag；</li>
 *     <li>P2(桶边界)：{@link #releaseIpSendQuota} 原先在释放时<b>重算当前桶</b>——预留在桶 H、释放在桶 H+1 时会误减新桶，
 *         且对新桶（不存在）{@code DECR} 会造出<b>无 TTL 的负计数</b> → 允许超发。改为预留时把确切桶键封进
 *         {@link IpQuotaReservation}，释放时只减<b>这些确切键</b>，且 Lua 内判「键存在且 &gt;0」才 DECR；</li>
 *     <li>P2(代际绑定)：{@link #releaseValidateAttempt} 原先无条件 {@code DECR} 计数键——同一 mobile+scene 两请求在途时，
 *         一个消费码触发 {@link #resetValidateAttempts}（删计数器）后若被新失败重建，旧请求的 stale 释放会误扣新计数器。
 *         改为「per-reservation token」：计数状态改用单个 Redis <b>Hash</b>（字段 {@code c}=计数，字段 {@code r:<token>}=预留标记），
 *         预留时写入唯一 token、释放时 {@code HDEL} 命中该 token 才 DECR；{@code reset}/TTL 过期会连同 token 一起清除，
 *         故 stale 释放（token 已不存在）无法再从重建后的新计数器扣减。单键 Hash → 天然 Cluster 安全。</li>
 * </ul>
 * <p>
 * ZS-LOGIN-004 codex r3 修复（r2 格式变更的<b>数据迁移/滚动部署兼容</b>，惰性读穿迁移保留计数与剩余 TTL）：
 * <ul>
 *     <li>P1(校验尝试 string→Hash)：升级到含既有计数器的环境时，未改名的
 *         {@code sms_code_validate_attempts:<mobile>:<scene>} 键仍是旧实现写入的 <b>string</b>，
 *         r1 的 Hash 命令对其抛 {@code WRONGTYPE} → 连正确码校验都失败直到旧计数器过期。
 *         现在所有触及该键的 Lua 脚本前置 {@link #LUA_MIGRATE_LEGACY_ATTEMPT_STRING} 惰性迁移：
 *         旧 string 值搬进 Hash 字段 {@code c} 并以 {@code PTTL/PEXPIRE} 保留剩余 TTL；无旧格式数据时是 no-op；</li>
 *     <li>P2(IP 配额 未标记→hash-tag 桶)：r1 给桶键加 {@code {...}} hash tag 后，活跃窗口内部署会抛弃
 *         旧未标记键下的计数——已耗尽配额的 IP 立即获得满额。现在 {@link #reserveIpSendQuota} 执行两键脚本前
 *         经 {@link #migrateLegacyIpBucket} 把旧未标记桶惰性播种进新 hash-tag 桶（seed-if-absent 保幂等，
 *         {@code PTTL/PEXPIRE} 保留剩余过期；旧新键在 Cluster 下可能异槽，故 Java 侧分步、不进两键脚本）；
 *         常态仅一次快速 {@code GET(null)}，no-op。</li>
 * </ul>
 * <p>
 * ZS-LOGIN-004 codex r4 修复（在 r3 惰性迁移基础上定向修 codex r3 揪出的 1×P1 + 1×P2，<b>保留惰性迁移方案</b>，
 * 不改成键版本化、不推倒 r3 成果；统一原则：<b>旧数据剩余 TTL &lt;= 0 → 视为已过期 → 丢弃，绝不制造无 TTL 的永久键</b>）：
 * <ul>
 *     <li>P1(滚动部署保留旧桶)：{@link #migrateLegacyIpBucket} 末尾不再无条件 {@code DEL} 旧未标记桶——
 *         改为 seed-only 保留旧桶，随其自身 TTL 自然过期。r3 的 delete 在滚动部署期会反复重置仍在写旧键的
 *         遗留实例配额：一旦 hash-tag 新桶存在，seed-if-absent 会跳过后续旧计数，但 delete 每次都执行——
 *         即便新版本请求本身被拒。已耗尽的 5 次发送桶可被「被拒的新版本请求」删掉，使遗留实例又能发 5 次；
 *         反复如此即无限绕过上限。seed-only 让旧桶随自身 TTL 自然过期，遗留实例仍可延续计数；</li>
 *     <li>P2(PTTL&lt;=0 视为过期，杜绝永久锁)：{@link #LUA_MIGRATE_LEGACY_ATTEMPT_STRING} 按 {@code PTTL} 分支——
 *         {@code PTTL > 0} 才搬迁旧值到 Hash 字段 {@code c} 并 {@code PEXPIRE} 保留剩余 TTL；
 *         {@code PTTL <= 0}（过期边界 0 / 异常持久 -1）<b>仅 DEL 丢弃旧锁</b>，视为已过期。r3 的
 *         「DEL + HSET c=legacy + 仅正数才 PEXPIRE」在过期边界会移除旧键的过期并把计数搬进无 TTL 的 Hash，
 *         若计数已达上限则之后每次校验被无限期拒绝（含正确码），成功消费的 reset 也无法运行；</li>
 *     <li>defensive(IP-seed 同 PTTL&lt;=0 处理，杜绝永久桶)：{@link #migrateLegacyIpBucket} 在 seed 之前加守卫——
 *         旧桶剩余 {@code PTTL <= 0}（无 TTL / 已过期边界）时视为已过期→<b>不播种</b>，交由后续
 *         {@link #SCRIPT_RESERVE_IP_QUOTA} 首次 {@code INCR} 时以完整桶 TTL 新建，避免播种出无 TTL 的永久新桶
 *         （与 P2 同一原则，防 r5 复审揪出同族漏洞）。</li>
 * </ul>
 *
 * @author ZS-LOGIN-004
 */
@Repository
public class SmsCodeSecurityRedisDAO {

    /**
     * 每 IP 发送计数的小时桶格式
     */
    private static final DateTimeFormatter HOUR_BUCKET = DateTimeFormatter.ofPattern("yyyyMMddHH");
    /**
     * 每 IP 发送计数的天桶格式
     */
    private static final DateTimeFormatter DAY_BUCKET = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 时间桶类型：小时
     */
    private static final String BUCKET_TYPE_HOUR = "h";
    /**
     * 时间桶类型：天
     */
    private static final String BUCKET_TYPE_DAY = "d";

    /**
     * 小时桶的 TTL，略大于 1 小时以覆盖桶边界的读写抖动
     */
    private static final Duration HOUR_BUCKET_TTL = Duration.ofHours(2);
    /**
     * 天桶的 TTL，略大于 1 天
     */
    private static final Duration DAY_BUCKET_TTL = Duration.ofDays(2);

    /**
     * ZS-LOGIN-004 r1：校验尝试计数 Hash 的「计数」字段名。
     */
    private static final String ATTEMPT_COUNT_FIELD = "c";
    /**
     * ZS-LOGIN-004 r1：校验尝试计数 Hash 的「预留标记」字段前缀（后接 per-reservation token）。
     */
    private static final String ATTEMPT_RESERVATION_FIELD_PREFIX = "r:";

    // ========== ZS-LOGIN-004：原子 Lua 脚本 ==========

    // ---- ZS-LOGIN-004 r3/r4：旧格式数据惰性迁移片段/脚本 ----

    /**
     * ZS-LOGIN-004 r3(P1) + r4(P2)：校验尝试计数「旧 string → Hash」惰性迁移片段（拼在触及该键的 Lua 脚本最前）。
     * <p>KEYS[1]=计数键。旧实现（r1 之前）写入的是 string 计数器，新实现的 {@code HGET/HINCRBY/HDEL}
     * 对其抛 {@code WRONGTYPE}。本片段仅在「键存在且 TYPE 为 string」时按 {@code PTTL} 分支处理：
     * <ul>
     *     <li>{@code PTTL > 0}：把旧值搬进 Hash 字段 {@code c} 并以 {@code PEXPIRE} 保留剩余 TTL
     *         （锁定期仍自第一次错误尝试起算，不因迁移顺延）；同时把 pttl 存入脚本局部 {@code zsLegacyPttl}，
     *         供后续 {@code HINCRBY} 脚本经 {@link #LUA_REAPPLY_MIGRATED_TTL} 在自增后重新回写；</li>
     *     <li>{@code PTTL <= 0}（过期边界 0 / 异常持久 -1）：<b>仅 DEL 丢弃旧锁</b>，视为已过期。
     *         后续脚本 {@code HGET} 得 nil → 计数按 0 起算，{@code HINCRBY} 首次以完整 lockDuration TTL 新建，
     *         绝不产生无 TTL 的永久 Hash（r4 修复：r3 的「DEL + HSET c=legacy + 仅正数才 PEXPIRE」
     *         在过期边界会移除旧键的过期并把计数搬进无 TTL 的 Hash，若计数已达上限则永久锁死）。</li>
     * </ul>
     * 无旧格式数据（常态）时仅一次 {@code EXISTS} 判断，no-op。单键操作 → Cluster 安全。
     * <p>兼容性说明：真实 Redis 的 {@code TYPE} 是状态回复，在 Lua 内为 {@code {ok='string'}} 表；
     * 测试基座（jedis-mock）则直接返回 Lua 字符串，故先归一化再比较，两种形态都命中。
     * <p>本片段将旧键剩余 TTL 捕获到脚本局部 {@code zsLegacyPttl}，供后续 {@code HINCRBY} 脚本（预留/自增）
     * 在自增<b>之后</b>用 {@link #LUA_REAPPLY_MIGRATED_TTL} 重新回写：真实 Redis 的 {@code HINCRBY} 保留 TTL，
     * 而测试基座会在 {@code HINCRBY} 时丢失 TTL，回写保证两种实现下迁移都保留剩余锁定时长（未迁移或
     * {@code PTTL<=0} 丢弃时为 0 → 跳过）。
     */
    private static final String LUA_MIGRATE_LEGACY_ATTEMPT_STRING =
            "local zsLegacyPttl = 0 "
          + "if redis.call('EXISTS', KEYS[1]) == 1 then "
          + "  local t = redis.call('TYPE', KEYS[1]) "
          + "  local tn = '' "
          + "  if type(t) == 'table' then tn = t.ok "
          + "  elseif type(t) == 'string' then tn = t end "
          + "  if tn == 'string' then "
          + "    local pttl = redis.call('PTTL', KEYS[1]) "
          + "    if pttl and pttl > 0 then "
          + "      local legacy = redis.call('GET', KEYS[1]) "
          + "      redis.call('DEL', KEYS[1]) "
          + "      if legacy and legacy ~= '' then "
          + "        redis.call('HSET', KEYS[1], '" + ATTEMPT_COUNT_FIELD + "', legacy) "
          + "        zsLegacyPttl = pttl "
          + "        redis.call('PEXPIRE', KEYS[1], pttl) "
          + "      end "
          + "    else "
          + "      redis.call('DEL', KEYS[1]) "
          + "    end "
          + "  end "
          + "end ";

    /**
     * ZS-LOGIN-004 r3(P1)：在 {@code HINCRBY} 之后重新回写迁移捕获的剩余 TTL（仅当发生过旧 string 迁移且
     * {@code PTTL>0} 时；r4：{@code PTTL<=0} 走 DEL 丢弃分支，{@code zsLegacyPttl} 保持 0 → 跳过回写）。
     * <p>拼在预留/自增脚本 {@code return} 之前；未迁移时 {@code zsLegacyPttl==0} → no-op，常态无任何影响。
     */
    private static final String LUA_REAPPLY_MIGRATED_TTL =
            "if zsLegacyPttl > 0 then redis.call('PEXPIRE', KEYS[1], zsLegacyPttl) end ";

    /**
     * ZS-LOGIN-004 r3(P1)：迁移前置 + 读取计数字段（供 {@link #getValidateAttempts} 使用）。
     * <p>取代裸 {@code opsForHash().get}——后者对升级遗留的旧 string 计数器抛 {@code WRONGTYPE}。
     * 返回迁移后的计数字段 {@code c}（字符串或 false/nil）；不存在时由调用方按 0 处理。单键 → Cluster 安全。
     */
    private static final RedisScript<String> SCRIPT_MIGRATE_AND_GET_ATTEMPTS = new DefaultRedisScript<>(
            LUA_MIGRATE_LEGACY_ATTEMPT_STRING
          + "return redis.call('HGET', KEYS[1], '" + ATTEMPT_COUNT_FIELD + "')",
            String.class);

    /**
     * P2-E：自增并在「首次创建」时原子设定过期（String 计数，用于每 IP 发送桶）。
     * <p>KEYS[1]=计数键，ARGV[1]=TTL 毫秒（<=0 表示不设过期）。返回自增后的值。
     * <p>合并 INCR + PEXPIRE 为单条脚本，消除「INCR 成功但 EXPIRE 未执行 → 永久键」的窗口。
     */
    private static final RedisScript<Long> SCRIPT_INCREMENT_WITH_EXPIRE = new DefaultRedisScript<>(
            "local n = redis.call('INCR', KEYS[1]) "
          + "if n == 1 then "
          + "  local ttl = tonumber(ARGV[1]) "
          + "  if ttl > 0 then redis.call('PEXPIRE', KEYS[1], ttl) end "
          + "end "
          + "return n",
            Long.class);

    /**
     * ZS-LOGIN-004 r1：校验尝试计数（Hash 字段 {@code c}）的自增 + 首次过期（用于 {@link #increaseValidateAttempts}）。
     * <p>KEYS[1]=计数 Hash 键，ARGV[1]=TTL 毫秒（<=0 表示不设过期）。返回自增后的计数。
     * <p>单键操作 → Redis Cluster 安全。
     */
    private static final RedisScript<Long> SCRIPT_HASH_INCREMENT_WITH_EXPIRE = new DefaultRedisScript<>(
            LUA_MIGRATE_LEGACY_ATTEMPT_STRING
          + "local ttl = tonumber(ARGV[1]) "
          + "local n = redis.call('HINCRBY', KEYS[1], '" + ATTEMPT_COUNT_FIELD + "', 1) "
          + "if n == 1 and ttl > 0 then redis.call('PEXPIRE', KEYS[1], ttl) end "
          + LUA_REAPPLY_MIGRATED_TTL
          + "return n",
            Long.class);

    /**
     * P2-C：原子「判上限 + 预留一次校验容量」（Hash：计数 + per-reservation token）。
     * <p>KEYS[1]=计数 Hash 键，ARGV[1]=上限，ARGV[2]=TTL 毫秒，ARGV[3]=本次预留的唯一 token。
     * <p>已达上限 → 返回 {@code -1} 且<b>不改动计数</b>（锁定期间的调用不得改变计数，避免被探测）；
     * 否则原子自增计数字段 {@code c}（首次自增时设 TTL）、并写入预留标记字段 {@code r:<token>}，返回新计数（>=1）。
     * <p>r1：预留标记与计数同处<b>单个 Hash 键</b>，故本脚本仍为单键 → Cluster 安全；标记用于把释放与本次预留代际绑定。
     */
    private static final RedisScript<Long> SCRIPT_RESERVE_VALIDATE_ATTEMPT = new DefaultRedisScript<>(
            LUA_MIGRATE_LEGACY_ATTEMPT_STRING
          + "local maxAttempts = tonumber(ARGV[1]) "
          + "local ttl = tonumber(ARGV[2]) "
          + "local token = ARGV[3] "
          + "local current = redis.call('HGET', KEYS[1], '" + ATTEMPT_COUNT_FIELD + "') "
          + "if not current then current = 0 else current = tonumber(current) end "
          + "if current >= maxAttempts then return -1 end "
          + "local n = redis.call('HINCRBY', KEYS[1], '" + ATTEMPT_COUNT_FIELD + "', 1) "
          + "if n == 1 and ttl > 0 then redis.call('PEXPIRE', KEYS[1], ttl) end "
          + "redis.call('HSET', KEYS[1], '" + ATTEMPT_RESERVATION_FIELD_PREFIX + "' .. token, '1') "
          + LUA_REAPPLY_MIGRATED_TTL
          + "return n",
            Long.class);

    /**
     * P2-C + r1(代际绑定)：释放一次已预留但未构成「失败」的校验容量（校验成功 / 验证码过期 / 已使用 / DB 查询异常）。
     * <p>KEYS[1]=计数 Hash 键，ARGV[1]=本次预留的 token。
     * <p>先 {@code HDEL r:<token>}：<b>命中（返回 1）才</b>说明这是「本次预留代际仍有效」的释放，进而对计数 {@code HINCRBY -1}
     * （减到 <=0 时删整个键，清除计数与全部残留标记）；<b>未命中（返回 0）</b>说明该预留已被 {@code reset}/TTL 过期清除
     * （计数器可能已被新失败重建），此时<b>不做任何扣减</b>，杜绝 stale 请求误扣重建后的新计数器。
     * <p>单键操作 → Cluster 安全。
     */
    private static final RedisScript<Long> SCRIPT_RELEASE_VALIDATE_ATTEMPT = new DefaultRedisScript<>(
            LUA_MIGRATE_LEGACY_ATTEMPT_STRING
          + "local token = ARGV[1] "
          + "if redis.call('HDEL', KEYS[1], '" + ATTEMPT_RESERVATION_FIELD_PREFIX + "' .. token) == 0 then return 0 end "
          + "local current = redis.call('HGET', KEYS[1], '" + ATTEMPT_COUNT_FIELD + "') "
          + "if not current then return 0 end "
          + "if tonumber(current) <= 0 then return 0 end "
          + "local n = redis.call('HINCRBY', KEYS[1], '" + ATTEMPT_COUNT_FIELD + "', -1) "
          + "if n <= 0 then redis.call('DEL', KEYS[1]) end "
          + "return n",
            Long.class);

    /**
     * P2-D：原子「检查并预留」每 IP 的小时桶 + 天桶两个发送配额。
     * <p>KEYS[1]=小时桶键，KEYS[2]=天桶键；ARGV[1]=小时上限，ARGV[2]=天上限，ARGV[3]=小时 TTL 毫秒，ARGV[4]=天 TTL 毫秒。
     * <p>两桶各自原子自增（首次设 TTL）；任一桶超限则<b>回滚两桶自增</b>并返回 {@code -1}（拒绝），
     * 否则返回预留后的小时桶计数（>=1，放行）。上限 <=0 视为「该维度不限制」。
     * 整段脚本单线程原子执行，故并发下恰有 {@code limit} 个请求能预留成功。
     * <p>r1(P1/Cluster)：两个 KEYS 均带 per-IP hash tag（{@code {...}}）→ 同槽 → 本两键脚本 Cluster 安全。
     */
    private static final RedisScript<Long> SCRIPT_RESERVE_IP_QUOTA = new DefaultRedisScript<>(
            "local hourLimit = tonumber(ARGV[1]) "
          + "local dayLimit = tonumber(ARGV[2]) "
          + "local hourTtl = tonumber(ARGV[3]) "
          + "local dayTtl = tonumber(ARGV[4]) "
          + "local h = redis.call('INCR', KEYS[1]) "
          + "if h == 1 and hourTtl > 0 then redis.call('PEXPIRE', KEYS[1], hourTtl) end "
          + "local d = redis.call('INCR', KEYS[2]) "
          + "if d == 1 and dayTtl > 0 then redis.call('PEXPIRE', KEYS[2], dayTtl) end "
          + "if hourLimit > 0 and h > hourLimit then "
          + "  redis.call('DECR', KEYS[1]) redis.call('DECR', KEYS[2]) return -1 "
          + "end "
          + "if dayLimit > 0 and d > dayLimit then "
          + "  redis.call('DECR', KEYS[1]) redis.call('DECR', KEYS[2]) return -1 "
          + "end "
          + "return h",
            Long.class);

    /**
     * P2-D + r1(桶边界)：释放此前预留的每 IP 配额（建码或派发失败时按需回滚，被拒的请求不占用配额）。
     * <p>KEYS[1]=预留时的小时桶键，KEYS[2]=预留时的天桶键（<b>由 {@link IpQuotaReservation} 携带的确切键</b>，不在释放时重算）。
     * <p>r1：仅当桶键<b>存在且计数 &gt; 0</b> 时才 DECR，减到 <=0 时删键——避免预留桶已过期/已滚动时对不存在的键 DECR
     * 造出「无 TTL 的负计数」而变相放大配额。两键均带同一 per-IP hash tag → Cluster 安全。
     */
    private static final RedisScript<Long> SCRIPT_RELEASE_IP_QUOTA = new DefaultRedisScript<>(
            "local h = redis.call('GET', KEYS[1]) "
          + "if h and tonumber(h) > 0 then "
          + "  local nh = redis.call('DECR', KEYS[1]) "
          + "  if nh <= 0 then redis.call('DEL', KEYS[1]) end "
          + "end "
          + "local d = redis.call('GET', KEYS[2]) "
          + "if d and tonumber(d) > 0 then "
          + "  local nd = redis.call('DECR', KEYS[2]) "
          + "  if nd <= 0 then redis.call('DEL', KEYS[2]) end "
          + "end "
          + "return 1",
            Long.class);

    /**
     * ZS-LOGIN-004 r3(P2)：仅当新键不存在时播种旧值并保留剩余 TTL（seed-if-absent，供每 IP 配额桶惰性迁移使用）。
     * <p>KEYS[1]=新 hash-tag 桶键，ARGV[1]=旧桶计数值，ARGV[2]=旧桶剩余 PTTL 毫秒（<=0 表示不设过期）。
     * <p>「不存在才播种」保证滚动部署/并发迁移下幂等：不会覆盖新键上其它实例已累加的计数。单键 → Cluster 安全。
     * <p>r4：调用方 {@link #migrateLegacyIpBucket} 已在 Java 侧加守卫，{@code PTTL<=0} 时直接 return 不调用本脚本，
     * 因此传入的 ARGV[2] 必为正——即便本脚本对 ARGV[2]<=0 走「不 PEXPIRE」的兜底分支也不会被触发产生永久桶。
     */
    private static final RedisScript<Long> SCRIPT_SEED_IF_ABSENT = new DefaultRedisScript<>(
            "if redis.call('EXISTS', KEYS[1]) == 0 then "
          + "  redis.call('SET', KEYS[1], ARGV[1]) "
          + "  if tonumber(ARGV[2]) > 0 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end "
          + "end "
          + "return 1",
            Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // ========== 验证码校验失败次数（暴力破解防护） ==========

    /**
     * 累加一次「验证码不匹配」的失败尝试，并返回累加后的次数。
     * <p>
     * ZS-LOGIN-004 P2-E：自增与「首次创建时的过期设定」合并为<b>单个 Lua 脚本</b>原子执行，
     * 消除原实现「{@code INCR} 成功后、{@code EXPIRE} 之前进程停止/通信失败 → 计数键永无 TTL」的窗口。
     * TTL 仍<b>只在首次计数（返回值 == 1）时设定</b>：锁定期自「第一次错误尝试」起算并保持固定长度，
     * 攻击者无法通过持续试错把锁定窗口无限顺延。
     * <p>
     * ZS-LOGIN-004 r1：计数状态改用单个 Redis Hash（字段 {@code c}）承载，与预留标记同键，故仍为单键原子操作。
     *
     * @param mobile       手机号
     * @param scene        验证码场景
     * @param lockDuration 锁定时长（即计数键的 TTL）；为空或非正数时不设过期
     * @return 累加后的失败次数
     */
    public long increaseValidateAttempts(String mobile, Integer scene, Duration lockDuration) {
        String redisKey = formatValidateAttemptsKey(mobile, scene);
        long ttlMillis = toTtlMillis(lockDuration);
        Long result = stringRedisTemplate.execute(SCRIPT_HASH_INCREMENT_WITH_EXPIRE,
                Collections.singletonList(redisKey), String.valueOf(ttlMillis));
        return result == null ? 0L : result;
    }

    /**
     * ZS-LOGIN-004 P2-C：原子「判上限 + 预留一次校验容量」。
     * <p>用于取代「先 {@link #getValidateAttempts} 读计数、判上限，再在失败后 {@link #increaseValidateAttempts} 自增」
     * 这一非原子的 check-then-act——并发下多个猜测都会读到低于上限而全部抵达 DB。
     * <p>r1(代际绑定)：预留成功时把唯一 token 写入计数 Hash，返回的 {@link ValidateAttemptReservation} 携带该 token；
     * 释放须凭此 token，{@code reset}/TTL 过期清除 token 后 stale 释放不再误扣重建的计数器。
     *
     * @param mobile       手机号
     * @param scene        验证码场景
     * @param maxAttempts  失败尝试上限；<=0 表示不启用限制，返回 {@link ValidateAttemptReservation#isReservedSlot()} 为 false 的「放行」预留
     * @param lockDuration 锁定时长（首次预留时设为计数键 TTL）
     * @return 预留结果：{@link ValidateAttemptReservation#isRejected()} 为 true 表示已达上限（锁定，计数不变，调用方应直接拒绝）；
     *         {@link ValidateAttemptReservation#isReservedSlot()} 为 true 表示已成功预留一个名额（放行，需凭 token 释放）；
     *         两者皆 false 表示未启用限制（放行、不计数、无需释放）
     */
    public ValidateAttemptReservation reserveValidateAttempt(String mobile, Integer scene, int maxAttempts, Duration lockDuration) {
        if (maxAttempts <= 0) {
            return ValidateAttemptReservation.disabled(mobile, scene);
        }
        String redisKey = formatValidateAttemptsKey(mobile, scene);
        long ttlMillis = toTtlMillis(lockDuration);
        String token = UUID.randomUUID().toString();
        Long result = stringRedisTemplate.execute(SCRIPT_RESERVE_VALIDATE_ATTEMPT,
                Collections.singletonList(redisKey), String.valueOf(maxAttempts), String.valueOf(ttlMillis), token);
        if (result == null) {
            return ValidateAttemptReservation.disabled(mobile, scene);
        }
        if (result < 0) {
            return ValidateAttemptReservation.rejected(mobile, scene);
        }
        return ValidateAttemptReservation.reserved(mobile, scene, result, token);
    }

    /**
     * ZS-LOGIN-004 P2-C：释放一次「已预留但未构成失败」的校验容量。
     * <p>在校验成功、验证码过期、验证码已使用、DB 查询异常等<b>非攻击信号</b>的路径调用，把 {@link #reserveValidateAttempt}
     * 预留的名额退还，保证只有「验证码不匹配 / 错场景」才真正累加失败计数。
     * <p>r1(代际绑定)：仅当预留 token 仍存在于计数 Hash（即本次预留代际未被 {@code reset}/过期清除）时才扣减，
     * 否则静默跳过——避免 stale 请求误扣重建后的新计数器。传入 {@code null} 或非预留态（未启用限制 / 已锁定）时不操作。
     */
    public void releaseValidateAttempt(ValidateAttemptReservation reservation) {
        if (reservation == null || !reservation.isReservedSlot()) {
            return;
        }
        String redisKey = formatValidateAttemptsKey(reservation.getMobile(), reservation.getScene());
        stringRedisTemplate.execute(SCRIPT_RELEASE_VALIDATE_ATTEMPT,
                Collections.singletonList(redisKey), reservation.getToken());
    }

    /**
     * 获得当前失败尝试次数；不存在时返回 0。
     * <p>ZS-LOGIN-004 r3(P1)：改走「迁移前置 + {@code HGET}」的 Lua 脚本（{@link #SCRIPT_MIGRATE_AND_GET_ATTEMPTS}），
     * 不再用裸 {@code opsForHash().get}——后者对升级遗留的旧 string 计数器抛 {@code WRONGTYPE}。
     */
    public long getValidateAttempts(String mobile, Integer scene) {
        String value = stringRedisTemplate.execute(SCRIPT_MIGRATE_AND_GET_ATTEMPTS,
                Collections.singletonList(formatValidateAttemptsKey(mobile, scene)));
        return parseCount(value);
    }

    /**
     * 清零失败尝试次数。<b>仅允许在「验证码被成功消费」时调用</b>，
     * 即只有合法用户走通一次完整校验才能解锁；不对外暴露任何「按时间/按请求」的重置入口。
     * <p>r1：删除整个计数 Hash，一并清除计数与全部 per-reservation token 标记——于是在途请求的 stale 释放
     * 因 token 已不存在而被 {@link #SCRIPT_RELEASE_VALIDATE_ATTEMPT} 跳过，不会误扣此后重建的新计数器。
     */
    public void resetValidateAttempts(String mobile, Integer scene) {
        stringRedisTemplate.delete(formatValidateAttemptsKey(mobile, scene));
    }

    /**
     * 获得失败尝试计数键的剩余锁定时长；键不存在或无 TTL 时返回 {@link Duration#ZERO}。
     * <p>暴露此方法是为了让测试可以断言「锁定确实会自动到期」，而非依赖人工介入。
     */
    public Duration getValidateAttemptsTtl(String mobile, Integer scene) {
        Long seconds = stringRedisTemplate.getExpire(formatValidateAttemptsKey(mobile, scene), TimeUnit.SECONDS);
        return seconds == null || seconds <= 0 ? Duration.ZERO : Duration.ofSeconds(seconds);
    }

    // ========== 每 IP 短信发送频控 ==========

    /**
     * ZS-LOGIN-004 P2-D：在持久化与派发<b>之前</b>，原子地检查并预留每 IP 的小时桶 + 天桶两个发送配额。
     * <p>取代原「先 {@code validateIpSendLimit} 读两桶计数判上限、建码发短信后再各自 {@code INCR} 且不检查结果」
     * 的非原子流程——并发下多个请求都会通过校验、全部建码派发、超发配额。
     * <p>r1(桶边界)：返回的 {@link IpQuotaReservation} 携带本次预留的<b>确切桶键</b>（含时间桶后缀），
     * 释放须凭此对象，确保跨小时/午夜边界失败时回滚的是<b>预留时的桶</b>而非当前桶。
     * <p>r3(P2 迁移)：执行两键脚本前对小时/天桶各做一次「旧未标记键 → 新 hash-tag 键」惰性播种
     * （{@link #migrateLegacyIpBucket}），保留活跃桶计数与剩余过期——否则在活跃窗口内部署时，
     * 键格式变更会抛弃旧键下计数，已耗尽配额的 IP 立即获得满额。无旧键时仅一次快速 GET(null)，no-op。
     * <p>r4(P1)：迁移改为 seed-only——保留旧未标记桶随其自身 TTL 自然过期，不再 delete，杜绝滚动部署期
     * 反复重置遗留实例配额的可重复绕过。详见 {@link #migrateLegacyIpBucket}。
     *
     * @param ip        客户端 IP（配额维度键）
     * @param hourLimit 每小时上限；<=0 表示该维度不限制
     * @param dayLimit  每天上限；<=0 表示该维度不限制
     * @return 预留结果：{@link IpQuotaReservation#isRejected()} 为 true 表示任一配额超限（已回滚，拒绝）；
     *         否则为放行，{@link IpQuotaReservation#getHourCount()} 为预留后的小时桶计数
     */
    public IpQuotaReservation reserveIpSendQuota(String ip, int hourLimit, int dayLimit) {
        String hourBucket = currentHourBucket();
        String dayBucket = currentDayBucket();
        // r3(P2) + r4(P1)：先把旧未标记桶的活跃计数惰性播种进新 hash-tag 桶（常态 no-op）；seed-only 保留旧桶。
        // 旧/新键在 Cluster 下可能异槽，故必须 Java 侧分步迁移，不能并入下方两键脚本。
        migrateLegacyIpBucket(formatLegacyIpSendCountKey(ip, BUCKET_TYPE_HOUR, hourBucket),
                formatIpSendCountKey(ip, BUCKET_TYPE_HOUR, hourBucket));
        migrateLegacyIpBucket(formatLegacyIpSendCountKey(ip, BUCKET_TYPE_DAY, dayBucket),
                formatIpSendCountKey(ip, BUCKET_TYPE_DAY, dayBucket));
        List<String> keys = Arrays.asList(
                formatIpSendCountKey(ip, BUCKET_TYPE_HOUR, hourBucket),
                formatIpSendCountKey(ip, BUCKET_TYPE_DAY, dayBucket));
        Long result = stringRedisTemplate.execute(SCRIPT_RESERVE_IP_QUOTA, keys,
                String.valueOf(hourLimit), String.valueOf(dayLimit),
                String.valueOf(HOUR_BUCKET_TTL.toMillis()), String.valueOf(DAY_BUCKET_TTL.toMillis()));
        long count = result == null ? 0L : result;
        if (count < 0) {
            return IpQuotaReservation.rejected();
        }
        return IpQuotaReservation.reserved(keys, count);
    }

    /**
     * ZS-LOGIN-004 P2-D：释放此前预留的每 IP 配额（建码或派发失败时按需回滚，被拒的请求不占用配额）。
     * <p>r1(桶边界)：只减 {@code reservation} 携带的<b>预留时确切桶键</b>，且 Lua 内判「键存在且 &gt;0」才 DECR，
     * 避免误减当前（新）桶或对不存在的键造出无 TTL 负计数。传入 {@code null} 或已拒绝的预留时不操作。
     */
    public void releaseIpSendQuota(IpQuotaReservation reservation) {
        if (reservation == null || !reservation.isReserved()) {
            return;
        }
        stringRedisTemplate.execute(SCRIPT_RELEASE_IP_QUOTA, reservation.getKeys());
    }

    /**
     * 获得该 IP 在当前小时桶内已发送的数量。
     */
    public long getIpSendCountPerHour(String ip) {
        return parseCount(stringRedisTemplate.opsForValue()
                .get(formatIpSendCountKey(ip, BUCKET_TYPE_HOUR, currentHourBucket())));
    }

    /**
     * 累加该 IP 当前小时桶的发送计数，并返回累加后的值。
     * <p>ZS-LOGIN-004 P2-E：自增与首次过期设定合并为单个 Lua 脚本原子执行。
     */
    public long increaseIpSendCountPerHour(String ip) {
        return increaseIpSendCount(ip, BUCKET_TYPE_HOUR, currentHourBucket(), HOUR_BUCKET_TTL);
    }

    /**
     * 获得该 IP 在当前天桶内已发送的数量。
     */
    public long getIpSendCountPerDay(String ip) {
        return parseCount(stringRedisTemplate.opsForValue()
                .get(formatIpSendCountKey(ip, BUCKET_TYPE_DAY, currentDayBucket())));
    }

    /**
     * 累加该 IP 当前天桶的发送计数，并返回累加后的值。
     * <p>ZS-LOGIN-004 P2-E：自增与首次过期设定合并为单个 Lua 脚本原子执行。
     */
    public long increaseIpSendCountPerDay(String ip) {
        return increaseIpSendCount(ip, BUCKET_TYPE_DAY, currentDayBucket(), DAY_BUCKET_TTL);
    }

    private long increaseIpSendCount(String ip, String bucketType, String bucket, Duration ttl) {
        String redisKey = formatIpSendCountKey(ip, bucketType, bucket);
        Long result = stringRedisTemplate.execute(SCRIPT_INCREMENT_WITH_EXPIRE,
                Collections.singletonList(redisKey), String.valueOf(ttl.toMillis()));
        return result == null ? 0L : result;
    }

    /**
     * 拼装失败尝试计数键。公开给测试用于直接断言键存在性与 TTL。
     */
    public static String formatValidateAttemptsKey(String mobile, Integer scene) {
        return String.format(SMS_CODE_VALIDATE_ATTEMPTS, mobile, scene);
    }

    /**
     * 拼装每 IP 发送计数（时间桶）键。
     * <p>ZS-LOGIN-004 r1(P1/Cluster)：把 IP 包进 {@code {...}} hash tag——Redis Cluster 只对 {@code {...}} 内的内容算槽，
     * 于是同一 IP 的小时桶键与天桶键落到<b>同一槽</b>，{@link #SCRIPT_RESERVE_IP_QUOTA} / {@link #SCRIPT_RELEASE_IP_QUOTA}
     * 这两键脚本不再抛 {@code CROSSSLOT}。所有构造该键处（配额预留/释放、计数读取/自增）统一走本方法，保证一致。
     */
    private static String formatIpSendCountKey(String ip, String bucketType, String bucket) {
        return String.format(SMS_CODE_SEND_IP_COUNT, "{" + ip + "}", bucketType, bucket);
    }

    /**
     * ZS-LOGIN-004 r3(P2)：拼装 r1 之前的<b>旧未标记</b>每 IP 发送计数键（IP 不带 {@code {...}} hash tag），
     * 仅用于惰性迁移时定位升级/滚动部署遗留的旧格式桶键。
     */
    private static String formatLegacyIpSendCountKey(String ip, String bucketType, String bucket) {
        return String.format(SMS_CODE_SEND_IP_COUNT, ip, bucketType, bucket);
    }

    /**
     * ZS-LOGIN-004 r3(P2) + r4(P1/defensive)：把旧未标记 IP 桶键的活跃计数与剩余过期惰性<b>播种</b>进新 hash-tag 桶键。
     * <p>旧键不存在（常态）时仅一次快速 {@code GET(null)} 即返回，no-op；存在时按 r4 修订的语义处理：
     * <ol>
     *     <li>读旧桶剩余 {@code PTTL}——若 {@code PTTL <= 0}（无 TTL 的异常持久键 / 已过期边界）<b>视为已过期，
     *         直接 return 不播种</b>（r4 defensive）：交由后续 {@link #SCRIPT_RESERVE_IP_QUOTA} 首次 {@code INCR}
     *         时以完整桶 TTL 新建，避免播种出无 TTL 的永久新桶（一旦计数达上限即造成永久限流）；</li>
     *     <li>{@code PTTL > 0} 时以 {@link #SCRIPT_SEED_IF_ABSENT} 播种新键（仅当新键不存在——滚动部署/并发下幂等，
     *         不覆盖新实例已累加的计数；以 {@code PEXPIRE} 保留剩余过期）；</li>
     *     <li><b>不再删除旧未标记桶</b>（r4 P1，seed-only）：旧桶随其自身 TTL 自然过期，滚动部署期遗留实例仍可
     *         继续在其上计数，杜绝「新版本请求（即便被拒）每次都删除旧桶 → 遗留实例下一次 INCR 从 1 重来 →
     *         反复重置配额」的可重复绕过。</li>
     * </ol>
     * 旧/新键在 Cluster 下可能异槽，故必须 Java 侧分步，不能并入两键 Lua 脚本。
     */
    private void migrateLegacyIpBucket(String legacyKey, String newKey) {
        String legacyValue = stringRedisTemplate.opsForValue().get(legacyKey);
        if (legacyValue == null || legacyValue.isEmpty()) {
            return;
        }
        Long legacyPttl = stringRedisTemplate.getExpire(legacyKey, TimeUnit.MILLISECONDS);
        // r4(defensive)：旧桶剩余 PTTL<=0（无 TTL 的异常持久键 / 已过期边界）视为已过期→不播种，
        // 避免 seed 出无 TTL 的永久新桶；交由后续 SCRIPT_RESERVE_IP_QUOTA 首次 INCR 时以完整桶 TTL 新建。
        if (legacyPttl == null || legacyPttl <= 0L) {
            return;
        }
        // r4(P1)：seed-only 保留旧桶随其自身 TTL 自然过期，不再 delete——杜绝滚动部署期反复重置遗留实例配额。
        stringRedisTemplate.execute(SCRIPT_SEED_IF_ABSENT, Collections.singletonList(newKey),
                legacyValue, String.valueOf(legacyPttl));
    }

    private static String currentHourBucket() {
        return LocalDateTime.now().format(HOUR_BUCKET);
    }

    private static String currentDayBucket() {
        return LocalDateTime.now().format(DAY_BUCKET);
    }

    private static long toTtlMillis(Duration duration) {
        return (duration == null || duration.isZero() || duration.isNegative()) ? 0L : duration.toMillis();
    }

    private static long parseCount(String value) {
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    // ========== ZS-LOGIN-004 r1：预留句柄（把「释放」与「预留时的确切状态/代际」绑定） ==========

    /**
     * 一次「每 IP 发送配额」预留的结果句柄。
     * <p>携带预留时使用的<b>确切桶键</b>（小时桶 + 天桶，含时间桶后缀），使 {@link #releaseIpSendQuota(IpQuotaReservation)}
     * 在跨小时/午夜边界失败时回滚的是<b>预留时的桶</b>而非释放时的当前桶。
     */
    public static final class IpQuotaReservation {

        private final List<String> keys;
        private final long hourCount;
        private final boolean rejected;

        private IpQuotaReservation(List<String> keys, long hourCount, boolean rejected) {
            this.keys = keys;
            this.hourCount = hourCount;
            this.rejected = rejected;
        }

        public static IpQuotaReservation rejected() {
            return new IpQuotaReservation(Collections.emptyList(), -1L, true);
        }

        public static IpQuotaReservation reserved(List<String> keys, long hourCount) {
            return new IpQuotaReservation(keys, hourCount, false);
        }

        /**
         * @return true 表示任一配额超限、已回滚、应拒绝本次发送
         */
        public boolean isRejected() {
            return rejected;
        }

        /**
         * @return true 表示两桶配额均已成功预留（放行），且持有可释放的确切桶键
         */
        public boolean isReserved() {
            return !rejected;
        }

        /**
         * @return 预留后的小时桶计数（>=1）；被拒时为 -1
         */
        public long getHourCount() {
            return hourCount;
        }

        /**
         * @return 本次预留使用的确切桶键（[小时桶键, 天桶键]），释放时据此回滚
         */
        public List<String> getKeys() {
            return keys;
        }
    }

    /**
     * 一次「校验尝试名额」预留的结果句柄。
     * <p>携带本次预留的唯一 token（写入计数 Hash 的 {@code r:<token>} 字段），使 {@link #releaseValidateAttempt} 只在
     * token 仍存在（即本次预留代际未被 {@code reset}/TTL 过期清除）时才扣减计数，杜绝 stale 释放误扣重建后的新计数器。
     */
    public static final class ValidateAttemptReservation {

        private final String mobile;
        private final Integer scene;
        private final long count;
        private final String token;

        private ValidateAttemptReservation(String mobile, Integer scene, long count, String token) {
            this.mobile = mobile;
            this.scene = scene;
            this.count = count;
            this.token = token;
        }

        public static ValidateAttemptReservation rejected(String mobile, Integer scene) {
            return new ValidateAttemptReservation(mobile, scene, -1L, null);
        }

        public static ValidateAttemptReservation disabled(String mobile, Integer scene) {
            return new ValidateAttemptReservation(mobile, scene, 0L, null);
        }

        public static ValidateAttemptReservation reserved(String mobile, Integer scene, long count, String token) {
            return new ValidateAttemptReservation(mobile, scene, count, token);
        }

        /**
         * @return true 表示已达上限、锁定、应直接拒绝（计数不变）
         */
        public boolean isRejected() {
            return count < 0;
        }

        /**
         * @return true 表示已成功预留一个名额（放行），释放须凭 {@link #getToken()}
         */
        public boolean isReservedSlot() {
            return count >= 1 && token != null;
        }

        public String getMobile() {
            return mobile;
        }

        public Integer getScene() {
            return scene;
        }

        /**
         * @return 预留后的计数（>=1）；未启用限制为 0；被拒为 -1
         */
        public long getCount() {
            return count;
        }

        /**
         * @return 本次预留的唯一 token；仅在 {@link #isReservedSlot()} 为 true 时非空
         */
        public String getToken() {
            return token;
        }
    }

}
