package cn.zszj.module.infra.service.file;

import cn.zszj.module.infra.controller.admin.file.vo.file.FileOrphanCleanupReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileOrphanCleanupRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileOrphanPreviewRespVO;

/**
 * 孤儿对象清理 Service（ZS-FILE-005.B）：存储有对象/DB 无记录的两步清理——
 * 先「预览」清点（只读），后按预览结果显式授权「清理」（执行前逐项重核验）。
 *
 * @author ZS-FILE-005.B
 */
public interface FileOrphanService {

    /**
     * 预览孤儿候选清单（只读清点，不做任何删除）。
     *
     * @param configId 存储配置编号；null 时使用 master 存储配置
     * @param prefix   清点前缀（相对路径前缀，空串=全部）；单次清点有上限，被截断时操作方
     *                 以逐段前缀续扫推进（codex r0 P2-2），保证后部孤儿可见
     * @return 候选清单（含截断标注）
     */
    FileOrphanPreviewRespVO preview(Long configId, String prefix);

    /**
     * 清理孤儿对象：逐 path 执行前重新清点并重核验（引用/保留期/活跃凭证/存储根隔离），
     * 命中任一保护即逐项记录跳过，不伪报全成功；对象已不存在的 path 幂等视为成功。
     *
     * @param reqVO 显式 path 授权（有界批次）
     * @return 逐项结果
     */
    FileOrphanCleanupRespVO cleanup(FileOrphanCleanupReqVO reqVO);

}
