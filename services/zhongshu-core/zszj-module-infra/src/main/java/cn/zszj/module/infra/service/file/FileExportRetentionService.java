package cn.zszj.module.infra.service.file;

import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportRetentionCleanupReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportRetentionCleanupRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportRetentionPreviewRespVO;

/**
 * 导出件用途保留期清理 Service（ZS-FILE-004.B）。
 *
 * <p>两步人工授权（循 ZS-FILE-005.B 孤儿清理先例）：预览只读候选（purpose='export' ∧ PUBLISHED ∧
 * 保留期已到，超上限如实截断）；清理以显式 id 授权（有界批次），执行前逐项重核验（窗口竞态防御），
 * 复用 deleteFile 既有保护（引用/中间态），不新增绕过面。</p>
 *
 * @author ZS-FILE-004.B
 */
public interface FileExportRetentionService {

    /**
     * 预览清理候选（只读，不改变任何状态）。
     *
     * @return 候选条目与截断标注
     */
    FileExportRetentionPreviewRespVO preview();

    /**
     * 按显式授权 id 清理过期导出件（逐项重核验 + 逐项记录成败）。
     *
     * @param req 授权的文件编号列表
     * @return 成功与失败明细
     */
    FileExportRetentionCleanupRespVO cleanup(FileExportRetentionCleanupReqVO req);

}
