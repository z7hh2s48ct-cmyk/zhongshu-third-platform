package cn.zszj.module.infra.framework.file.compensation;

import cn.zszj.framework.tenant.core.util.TenantUtils;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.framework.file.config.FileCompensationProperties;
import cn.zszj.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件删除自动补偿任务（ZS-FILE-005.B）：周期扫描超时 DELETING 记录并复用 .A 人工对账语义完成收敛。
 *
 * <p>合同（退出条件「超时/重启/重复清理不误删且最终对账一致」）：
 * <ul>
 *   <li><b>轮询补偿（收口论证：非 Outbox 事件）</b>——补偿对象是已落库的 DELETING 记录，DB 即事实源，
 *       周期扫描零漂移；Outbox 重试上限 5 次即 DEAD 转人工，存储故障持续时事件烧完重试反而退回人工，
 *       与「自动补偿最终收敛」相反；轮询每轮重读最新状态，天然无限退避重试（有效节奏 =
 *       max(扫描间隔, grace)）；崩溃恢复天然（无内存态，重启下轮重扫）；删除失败发生在业务事务之外，
 *       「事务内先写事件」合同无挂载点；</li>
 *   <li><b>多实例串行化——DB 条件 CAS 领取</b>（循 JOB-002 栅栏思想的轻量租约）：
 *       {@code UPDATE … SET deleting_time=now WHERE id=? AND status='DELETING' AND deleting_time<=graceBefore}
 *       恰好一个实例 affected=1；领取同时完成「租约」与「重试退避点推进」；慢执行者残余动作无害
 *       （对象删除幂等且 DELETING 期引用集不增长，记录移除条件化）；</li>
 *   <li><b>复用 .A 对账路径，不另写清理逻辑</b>——领取成功后调
 *       {@link FileService#reconcileCleanupFile(Long)}：引用保护（进行中交付会话阻塞）、
 *       对象确认缺失仅移记录、路径校验全部继承，补偿语义上就是「定时的人工对账」，
 *       不可能绕过 .A 已闭合竞态（兑换侧 DELETING 拒绝新建会话等）；</li>
 *   <li><b>逐项异常隔离</b>——单条失败不中断本轮其余候选；失败 WARN 仅记异常类名不落原文（脱敏红线）；
 *       整轮 try/catch：任务自身失败 ERROR 明示、不吞、不击穿调度线程；</li>
 *   <li><b>租户上下文</b>——调度线程无租户：候选扫描以 {@link TenantUtils#executeIgnore} 全局执行，
 *       逐记录领取/对账以 {@link TenantUtils#execute} 在记录自身租户内执行（循 JOB-002 dispatcher 模式）。</li>
 * </ul>
 */
@Slf4j
@Component
public class FileDeleteCompensationJob {

    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileService fileService;
    @Resource
    private FileCompensationProperties properties;

    /**
     * 周期补偿入口。间隔取 infra.file.compensation.interval-ms（保守占位，待实测回填）；
     * 开关关闭时为 no-op（代码默认 false，未配置环境不产生后台对象删除）。
     */
    @Scheduled(fixedDelayString = "${infra.file.compensation.interval-ms:1800000}")
    public void compensate() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            int[] summary = compensateOnce();
            if (summary[0] > 0) {
                log.info("[compensate][本轮补偿：候选 {} 个，领取 {} 个，收敛 {} 个]", summary[0], summary[1], summary[2]);
            }
        } catch (Exception ex) {
            // 失败不静默（循 JOB-004/OPS 惯例）：任务自身失败 ERROR 明示，不吞、不击穿调度线程
            log.error("[compensate][文件删除自动补偿任务失败，本轮中止（下一轮重试）: {}]",
                    ex.getClass().getName(), ex);
        }
    }

    /**
     * 执行一轮补偿（公开供单测直接调用；@Scheduled 在单测上下文不自动触发，保证确定性）。
     *
     * @return [候选数, 领取数, 收敛数]
     */
    public int[] compensateOnce() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("compensateOnce 必须在无事务上下文中调用：领取与对账为逐条独立操作");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime graceBefore = now.minusMinutes(properties.getGraceMinutes());
        int maxPerCycle = properties.getMaxPerCycle();
        // 调度线程无租户上下文：候选扫描全局执行（逐记录再进入其自身租户）
        List<FileDO> candidates = TenantUtils.executeIgnore(() ->
                fileMapper.selectCompensationCandidates(graceBefore, maxPerCycle));
        int claimed = 0;
        int converged = 0;
        for (FileDO candidate : candidates) {
            // 逐记录在其自身租户上下文内领取（infra_file 为租户表；循 JOB-002 dispatcher 租户模式）
            LocalDateTime claimTime = LocalDateTime.now();
            Integer affected = TenantUtils.execute(candidate.getTenantId(), () ->
                    fileMapper.claimDeletingForCompensation(candidate.getId(), graceBefore, claimTime));
            if (affected == null || affected == 0) {
                continue; // 败者：已被其他实例领取/状态已变/窗口未到
            }
            claimed++;
            try {
                // 复用 .A 人工对账路径：引用保护 + 重删 + 确认缺失仅移记录全继承
                TenantUtils.execute(candidate.getTenantId(), () -> {
                    fileService.reconcileCleanupFile(candidate.getId());
                    return null;
                });
                converged++;
            } catch (Exception ex) {
                // 逐项隔离：失败保留 DELETING 记录（deleting_time 已随领取前推构成退避），下轮 grace 后再试
                log.warn("[compensate][文件({}) 本轮补偿未收敛，保留 DELETING 记录: {}]",
                        candidate.getId(), ex.getClass().getSimpleName());
            }
        }
        return new int[]{candidates.size(), claimed, converged};
    }

}
