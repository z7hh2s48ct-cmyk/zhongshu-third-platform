package cn.zszj.module.system.service.organization;

import cn.hutool.core.util.ObjectUtil;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.datapermission.core.util.DataPermissionUtils;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.dal.mysql.membership.MembershipMapper;
import cn.zszj.module.system.dal.mysql.organization.OrganizationMapper;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;

/**
 * 组织 Service 实现类
 *
 * <p>ZS-IAM-002（D-09 FND-IAM-002）：单表 type + parent_id 自引用树；code 租户内唯一（逻辑删除后可重用，
 * 由 DB 部分唯一索引 uk_system_organization_code 兜底，应用层查重作友好提示）。
 *
 * <p><b>层级变更并发策略（codex r2/r3 P2）</b>：按操作类型分级，不统一拉高隔离级——
 * <ul>
 *   <li><b>重挂（updateOrganization）用 {@link Isolation#SERIALIZABLE}</b>：这是唯一能形成跨事务大环
 *       write-skew 的路径（两事务各锁不相交祖先集，其间他事务提交拼接成环）。行锁（升序锁完整祖先链）
 *       只能串行化「触及同一行」的直接冲突、防死锁，拦不住不相交集成环；PG SSI 可串行化快照隔离下，任何
 *       导致环的交错都形成读写反依赖环，DB 提交时中止其一（抛可重试并发异常），从根杜绝大环。两个重挂
 *       事务同为 SERIALIZABLE → SSI 一致，保证中止（不与 READ COMMITTED 写方混用而失效）。</li>
 *   <li><b>create/delete 保持 READ COMMITTED（默认）</b>：二者只挂叶子 / 删节点，不可能自成环，无需 SERIALIZABLE；
 *       且它们依赖「锁后新鲜复验」（create 复验父存在、delete 复验引用计数，均 #8b）。SERIALIZABLE 会把快照定格在
 *       阻塞于组织行锁之前，锁后计数/校验仍读旧快照——并发的 READ COMMITTED 任职写方提交后，delete 会误判零引用
 *       删除刚被引用的组织，SSI 因写方非 SERIALIZABLE 不保证中止（codex r3 P2）。READ COMMITTED 每语句取新快照，
 *       锁后读见最新提交，正确串行化。</li>
 * </ul>
 * 锁后成环校验统一用 selectByIdForUpdate 新鲜读（绕过 MyBatis SESSION 本地缓存，codex r2 P2）。组织层级变更是低频
 * 管理操作，重挂路径的 SERIALIZABLE 中止面可接受。
 *
 * @author ZS-IAM-002
 */
@Service
@Validated
public class OrganizationServiceImpl implements OrganizationService {

    /**
     * 祖先链遍历深度上限（codex r1 P2）：超过即无法在有限步内证明无环，一律 fail-closed 拒绝，绝不静默通过。
     */
    private static final int MAX_ANCESTOR_DEPTH = 256;

    @Resource
    private OrganizationMapper organizationMapper;

    @Resource
    private MembershipMapper membershipMapper;

    @Override
    // READ COMMITTED（默认隔离级，codex r3 P2）：create 只挂新叶子（无后代，不可能自成环），无需 SERIALIZABLE。
    // 父行经 lockParentForReference 加锁后，READ COMMITTED 的锁后读取见最新提交——若并发的父删除先持锁，
    // 本事务阻塞至其提交，随后新鲜读发现父已不存在即拒绝；SERIALIZABLE 会把快照定格在阻塞前，锁后校验读陈旧父状态。
    @Transactional(rollbackFor = Exception.class)
    public Long createOrganization(OrganizationDO organization) {
        // 校验类型、编码唯一、父组织存在
        validateType(organization.getType());
        validateCodeUnique(null, organization.getCode());
        // codex r1 P2：引用创建前锁父行，与父组织删除串行化——杜绝「删除见零引用后本事务再挂子」留下孤引用（无 FK 兜底）
        lockParentForReference(organization.getParentId());
        validateParentForCreateOrUpdate(null, organization.getParentId());
        // 插入
        organizationMapper.insert(organization);
        return organization.getId();
    }

    @Override
    // SERIALIZABLE 仅用于重挂（codex r2/r3 P2）：这是唯一能形成跨事务大环 write-skew 的路径——两并发重挂各锁
    // 不相交祖先集（T1{A,B}、T2{C,D}），行锁升序仅防死锁拦不住成环；PG SSI 下任何导致环的交错都形成读写反依赖环，
    // DB 提交时中止其一。两个重挂事务同为 SERIALIZABLE → SSI 一致，保证中止（不与 READ COMMITTED 写方混用而失效）。
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void updateOrganization(OrganizationDO organization) {
        // 校验存在
        validateOrganizationExists(organization.getId());
        validateType(organization.getType());
        validateCodeUnique(organization.getId(), organization.getCode());
        // codex r0 P2：层级变更串行化——先按 id 升序对「被改节点 + 新父节点」加行锁（固定锁序防死锁），
        // 再在受保护事务内做成环校验；杜绝两并发重挂（A→B 之下、B→A 之下）都读原始树、双双通过校验后留环的 write-skew。
        // 仅加事务不足以防止该 write-skew（两事务快照各自通过校验），必须锁行串行化。
        lockHierarchyForReparent(organization.getId(), organization.getParentId());
        validateParentForCreateOrUpdate(organization.getId(), organization.getParentId());
        // 更新
        organizationMapper.updateById(organization);
    }

    @Override
    // READ COMMITTED（默认隔离级，codex r3 P2）：delete 依赖「锁后新鲜复验引用计数」（#8b）。SERIALIZABLE 会把
    // 快照定格在阻塞于组织行锁之前——并发 createMembership/transferMembership（READ COMMITTED）持锁提交后，本事务
    // 获锁但 selectCountByOrganizationId 仍读旧快照返回 0，误删刚被引用的组织；SSI 因写方非 SERIALIZABLE 不保证中止
    // 该交错（回退 #8b）。READ COMMITTED 下每条语句取新快照，锁后计数见最新提交 → 正确拒绝删除有引用的组织。
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrganization(Long id) {
        // codex r1 P2：先锁自身行再计数，与引用创建（子组织 / 任职）串行化——无锁时并发 create 可在
        // 删除见零引用后插入引用，留下指向已删组织的孤引用（迁移未提供 FK 保护）；获锁后复验计数即准确。
        OrganizationDO locked = organizationMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw exception(ORGANIZATION_NOT_EXISTS);
        }
        // 校验无子组织
        if (organizationMapper.selectCountByParentId(id) > 0) {
            throw exception(ORGANIZATION_HAS_CHILDREN);
        }
        // 校验无任职成员（关闭数据权限、保留租户过滤，计数须全量）
        Long membershipCount = DataPermissionUtils.executeIgnore(() -> membershipMapper.selectCountByOrganizationId(id));
        if (membershipCount > 0) {
            throw exception(ORGANIZATION_HAS_MEMBERSHIPS);
        }
        organizationMapper.deleteById(id);
    }

    @Override
    public OrganizationDO getOrganization(Long id) {
        return organizationMapper.selectById(id);
    }

    @Override
    public List<OrganizationDO> getOrganizationListByType(Integer type) {
        return organizationMapper.selectListByType(type);
    }

    @Override
    public void validateOrganizationEnabled(Long id) {
        OrganizationDO organization = organizationMapper.selectById(id);
        if (organization == null) {
            throw exception(ORGANIZATION_NOT_EXISTS);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(organization.getStatus())) {
            throw exception(ORGANIZATION_NOT_ENABLE, organization.getName());
        }
    }

    @Override
    public void validateOrganizationEnabledForUpdate(Long id) {
        // codex r1 P2：写路径（任职创建 / 转岗）引用组织前锁行，与组织删除串行化，随后校验存在 + 开启。
        // 与只读的 validateOrganizationEnabled 区分：本方法持 SELECT ... FOR UPDATE，须在 @Transactional 内调用。
        OrganizationDO organization = organizationMapper.selectByIdForUpdate(id);
        if (organization == null) {
            throw exception(ORGANIZATION_NOT_EXISTS);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(organization.getStatus())) {
            throw exception(ORGANIZATION_NOT_ENABLE, organization.getName());
        }
    }

    private void validateOrganizationExists(Long id) {
        if (id != null && organizationMapper.selectById(id) == null) {
            throw exception(ORGANIZATION_NOT_EXISTS);
        }
    }

    /**
     * 引用创建（挂子组织）前锁父行（codex r1 P2）：与父组织删除串行化。根 / 空父无需锁。
     */
    private void lockParentForReference(Long parentId) {
        if (parentId != null && !Objects.equals(parentId, OrganizationDO.PARENT_ID_ROOT)) {
            organizationMapper.selectByIdForUpdate(parentId);
        }
    }

    /**
     * 重挂父级前对涉及行加锁（codex r0 P2 / r1 P2）：锁「被改节点及其祖先链 ∪ 新父节点及其祖先链」，
     * 全局按 id 升序取锁（{@link TreeSet} 天然有序）防死锁。
     *
     * <p>r1 修正：仅锁「被改节点 + 新父节点」不足以防大环——并发 (A 挂 D 下) 与 (C 挂 B 下) 各锁 {A,D}、{C,B}
     * 互不相交，双双读原始树通过校验后留 A→D→C→B→A。锁完整祖先链后，任何触及共同祖先的并发重挂都会在同一行
     * 上串行化（上例两事务均锁到 A），后到者阻塞至前者提交，再基于最新树做成环校验即被拒。
     */
    private void lockHierarchyForReparent(Long id, Long parentId) {
        Set<Long> lockIds = new TreeSet<>();
        collectAncestorChain(id, lockIds);
        if (parentId != null && !Objects.equals(parentId, OrganizationDO.PARENT_ID_ROOT)) {
            collectAncestorChain(parentId, lockIds);
        }
        for (Long lockId : lockIds) {
            organizationMapper.selectByIdForUpdate(lockId);
        }
    }

    /**
     * 自 {@code startId} 向上收集祖先链（含自身）到 {@code sink}。visited 去重检测库中既有环 + 深度上限
     * fail-safe 终止，防止既有环导致死循环（成环本身由 {@link #validateParentForCreateOrUpdate} 判定并拒绝）。
     */
    private void collectAncestorChain(Long startId, Set<Long> sink) {
        Set<Long> visited = new HashSet<>();
        Long cursor = startId;
        while (cursor != null && !Objects.equals(cursor, OrganizationDO.PARENT_ID_ROOT)) {
            if (!visited.add(cursor) || visited.size() > MAX_ANCESTOR_DEPTH) {
                break;
            }
            sink.add(cursor);
            OrganizationDO node = organizationMapper.selectById(cursor);
            cursor = ObjectUtil.isNull(node) ? null : node.getParentId();
        }
    }

    private void validateType(Integer type) {
        boolean valid = false;
        for (Integer value : OrganizationTypeEnum.ARRAYS) {
            if (Objects.equals(value, type)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw exception(ORGANIZATION_TYPE_INVALID, type);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        // 空编码不参与唯一校验（DB 部分唯一索引 WHERE code <> '' 同口径）
        if (code == null || code.isEmpty()) {
            return;
        }
        OrganizationDO organization = organizationMapper.selectByCode(code);
        if (organization == null) {
            return;
        }
        if (id == null || !organization.getId().equals(id)) {
            throw exception(ORGANIZATION_CODE_DUPLICATE, code);
        }
    }

    private void validateParentForCreateOrUpdate(Long id, Long parentId) {
        if (parentId == null || Objects.equals(parentId, OrganizationDO.PARENT_ID_ROOT)) {
            return;
        }
        // 父组织必须存在（codex r2 P2：锁后用 selectByIdForUpdate 新鲜读，绕过 MyBatis SESSION 本地缓存——
        // create 路径父行已由 lockParentForReference 加锁、update 路径已由 lockHierarchyForReparent 加锁，此处重入不阻塞）
        OrganizationDO parent = organizationMapper.selectByIdForUpdate(parentId);
        if (parent == null) {
            throw exception(ORGANIZATION_PARENT_NOT_EXISTS);
        }
        if (id == null) {
            return;
        }
        // 不能设置自己为父组织
        if (Objects.equals(id, parentId)) {
            throw exception(ORGANIZATION_PARENT_ERROR);
        }
        // 不能设置自己的子组织为父组织：自 parentId 向上遍历祖先链，若遇 id 则成环。
        // codex r1 P2：遍历必须走到根，用 visited 去重检测既有环 + 深度上限「耗尽即拒」——原实现 guard 耗尽后
        // 静默通过，把根挂到超过上限深度的最深后代下会漏检成环（无并发亦成立）。命中环或无法证明无环一律拒绝。
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && !Objects.equals(cursor, OrganizationDO.PARENT_ID_ROOT)) {
            if (Objects.equals(cursor, id)) {
                throw exception(ORGANIZATION_PARENT_ERROR);
            }
            if (!visited.add(cursor) || visited.size() > MAX_ANCESTOR_DEPTH) {
                // visited 命中 = 库中既有环；深度超上限 = 有限步内无法证明无环 → fail-closed 拒绝，绝不静默通过
                throw exception(ORGANIZATION_PARENT_ERROR);
            }
            // codex r2 P2：锁后新鲜读（selectByIdForUpdate 绕过 SESSION 本地缓存），祖先行本事务已加锁 → 重入不阻塞，
            // 但确保成环校验基于锁后最新树而非锁前缓存的陈旧树（配合 SERIALIZABLE 双重兜底 write-skew）。
            OrganizationDO node = organizationMapper.selectByIdForUpdate(cursor);
            cursor = ObjectUtil.isNull(node) ? null : node.getParentId();
        }
    }

}
