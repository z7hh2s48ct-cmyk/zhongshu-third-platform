package cn.zszj.module.infra.dal.mysql.file;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.infra.controller.admin.file.vo.file.FilePageReqVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件操作 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FileMapper extends BaseMapperX<FileDO> {

    default PageResult<FileDO> selectPage(FilePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FileDO>()
                .likeIfPresent(FileDO::getPath, reqVO.getPath())
                .likeIfPresent(FileDO::getType, reqVO.getType())
                .betweenIfPresent(FileDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(FileDO::getId));
    }

    default FileDO selectLatestByConfigIdAndPath(Long configId, String path) {
        return selectLastOne(new LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getConfigId, configId)
                .eq(FileDO::getPath, path)
                .orderByAsc(FileDO::getId));
    }


    /**
     * 按资产状态查询（ZS-FILE-005.A 人工对账：DELETING 可恢复记录）
     */
    /**
     * 按编号加行锁读取（FOR UPDATE，ZS-FILE-005.A codex r3 P2：兑换持锁至提交，
     * 与删除侧的中间态转移/引用检查串行化）
     */
    default FileDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getId, id)
                .last("FOR UPDATE"));
    }

    default List<FileDO> selectListByStatus(String status) {
        return selectList(new LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getStatus, status)
                .orderByDesc(FileDO::getId));
    }

    // ========== ZS-FILE-005.B：自动补偿（超时 DELETING 的扫描 / CAS 领取 / 条件移除） ==========

    /**
     * 补偿候选（ZS-FILE-005.B）：DELETING 且 deleting_time 已超时（≤ graceBefore），稳定 id 排序、有界。
     * deleting_time 为 NULL 的记录不进入候选（其进入时刻由迁移 V20260916.102 按 update_time 回填，超时语义保持确定）。
     * 调度线程无租户上下文，调用方须以 TenantUtils.executeIgnore 包裹。
     */
    default List<FileDO> selectCompensationCandidates(java.time.LocalDateTime graceBefore, int limit) {
        return selectList(new LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getStatus, FileDO.STATUS_DELETING)
                .isNotNull(FileDO::getDeletingTime)
                .le(FileDO::getDeletingTime, graceBefore)
                .orderByAsc(FileDO::getId)
                .last("LIMIT " + limit));
    }

    /**
     * 补偿领取 CAS（ZS-FILE-005.B，循 JOB-002 栅栏思想的轻量租约）：仅当记录仍处 DELETING
     * 且超时窗口未被领取（deleting_time ≤ graceBefore）时，把 deleting_time 前推至 now——
     * 一个条件更新同时完成「领取租约」与「重试退避点推进」；并发实例/人工对账败者 affected=0 跳过。
     */
    default int claimDeletingForCompensation(Long id, java.time.LocalDateTime graceBefore,
                                             java.time.LocalDateTime now) {
        return update(null, new LambdaUpdateWrapper<FileDO>()
                .set(FileDO::getDeletingTime, now)
                .eq(FileDO::getId, id)
                .eq(FileDO::getStatus, FileDO.STATUS_DELETING)
                .le(FileDO::getDeletingTime, graceBefore));
    }

    /**
     * 条件化记录移除（ZS-FILE-005.B 硬化）：仅当记录仍处 DELETING 时移除——
     * 对象删除与记录移除之间若发生引用回退/对账交错，绝不移除非 DELETING 记录（重复清理不误删）。
     */
    default int deleteByIdIfStillDeleting(Long id) {
        return delete(new LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getId, id)
                .eq(FileDO::getStatus, FileDO.STATUS_DELETING));
    }

    /**
     * 引用存在性统计（ZS-FILE-005.B 孤儿核验）：同 config+path 的资产记录数——
     * 调用方必须以 TenantUtils.executeIgnore 包裹做【跨租户全局】核验：若被租户过滤，
     * 他租户在同 config+path 的记录会被误判为孤儿（不跨技术租户误删的反面）。
     */
    default Long selectCountByConfigIdAndPathIgnoreTenant(Long configId, String path) {
        return selectCount(new LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getConfigId, configId)
                .eq(FileDO::getPath, path));
    }

}
