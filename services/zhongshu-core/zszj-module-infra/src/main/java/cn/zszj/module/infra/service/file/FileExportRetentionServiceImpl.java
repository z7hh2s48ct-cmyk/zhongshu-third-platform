package cn.zszj.module.infra.service.file;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportRetentionCleanupReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportRetentionCleanupRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportRetentionItemRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportRetentionPreviewRespVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.framework.file.config.FileExportProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_EXPORT_RETENTION_BATCH_EXCEED;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_NOT_EXISTS;

/**
 * 导出件用途保留期清理 Service 实现（ZS-FILE-004.B）。
 *
 * <p>合同（FILE-005.B 计划 L46/L97 明文移交「导出文件按用途保留期清理」）：
 * <ul>
 *     <li>preview 只读：候选 = {@code purpose='export' ∧ status='PUBLISHED' ∧
 *     retention_expire_time <= now}（租户内；LIMIT 上限 + truncated 如实标注，循孤儿清理
 *     截断先例——宁可如实提示，不假报全量）；</li>
 *     <li>cleanup 显式授权：入参显式 fileIds（有界批次 ≤ 上限，超限整批错误码）；执行前逐项
 *     <b>重读 + 重核验</b>（预览→清理窗口竞态防御：仍是 export ∧ 仍 PUBLISHED ∧ 仍过期；
 *     任一不再成立即记失败跳过携原因，不伪报全成功）；</li>
 *     <li>清理走 {@link FileService#deleteFile}——引用保护（活跃交付会话=FILE_DELETE_REFERENCED）/
 *     DELETING 可恢复中间态/自动补偿框架全继承，不新增绕过面；</li>
 *     <li>失败留痕受控描述（既有保护携错误码便于对账；存储异常仅落异常类名，循 JOB-002/004 脱敏红线）。</li>
 * </ul>
 *
 * @author ZS-FILE-004.B
 */
@Slf4j
@Service
@Validated
public class FileExportRetentionServiceImpl implements FileExportRetentionService {

    @Resource
    private FileService fileService;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private FileExportProperties exportProperties;

    @Override
    public FileExportRetentionPreviewRespVO preview() {
        int max = exportProperties.getPreviewMaxItems();
        List<FileDO> candidates = fileMapper.selectExportRetentionCandidates(LocalDateTime.now(), max);

        FileExportRetentionPreviewRespVO resp = new FileExportRetentionPreviewRespVO();
        // 达到上限即如实标注截断（无「是否还有更多」的额外探测——保守方向：宁可多提示截断）
        resp.setTruncated(candidates.size() >= max);
        resp.setItems(candidates.stream().map(this::toItem).toList());
        log.info("[preview][导出件保留期预览：候选 {} 个，截断={}]", candidates.size(), resp.isTruncated());
        return resp;
    }

    @Override
    public FileExportRetentionCleanupRespVO cleanup(FileExportRetentionCleanupReqVO req) {
        int maxIds = exportProperties.getCleanupMaxIds();
        List<Long> fileIds = req.getFileIds();
        if (fileIds != null && fileIds.size() > maxIds) {
            throw exception(FILE_EXPORT_RETENTION_BATCH_EXCEED, fileIds.size(), maxIds);
        }
        FileExportRetentionCleanupRespVO resp = new FileExportRetentionCleanupRespVO();
        if (fileIds == null) {
            return resp;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long fileId : fileIds) {
            try {
                // 执行前逐项重读 + 重核验（任一条件不再成立即记失败跳过，不伪报全成功）
                FileDO file = fileId == null ? null : fileMapper.selectById(fileId);
                if (file == null) {
                    // 记录不存在无法证明「属于本通道的过期导出件」（可能已被清理或为混入编号）——
                    // 保守记失败携错误码，不假报幂等成功
                    addFailure(resp, fileId, "FILE_NOT_EXISTS(" + FILE_NOT_EXISTS.getCode()
                            + ")：记录不存在（可能已被清理或非本通道编号）");
                    continue;
                }
                if (!FileDO.PURPOSE_EXPORT.equals(file.getPurpose())) {
                    addFailure(resp, fileId, "FILE_EXPORT_PURPOSE_NOT_EXPORT：非导出件（用途门），不在本通道清理范围");
                    continue;
                }
                if (!FileDO.STATUS_PUBLISHED.equals(file.getStatus())) {
                    addFailure(resp, fileId, "FILE_EXPORT_STATUS_NOT_PUBLISHED：记录不处于 PUBLISHED（中间态保护，进行中删除不由本通道介入）");
                    continue;
                }
                if (file.getRetentionExpireTime() == null || file.getRetentionExpireTime().isAfter(now)) {
                    addFailure(resp, fileId, "FILE_EXPORT_RETENTION_NOT_EXPIRED：保留期未到，跳过（边界保护）");
                    continue;
                }
                // 复用既有删除链：org 门 / 引用保护 / DELETING 中间态 / 对象删除 / 条件记录移除全继承
                fileService.deleteFile(fileId);
                resp.getSuccessIds().add(fileId);
                log.info("[cleanup][导出件({}) 已按用途保留期清理]", fileId);
            } catch (ServiceException ex) {
                // 既有保护（如引用保护 FILE_DELETE_REFERENCED）逐项留痕：携错误码便于对账
                addFailure(resp, fileId, "[code=" + ex.getCode() + "] " + ex.getMessage());
                log.warn("[cleanup][导出件({}) 清理失败: [code={}] {}]", fileId, ex.getCode(), ex.getMessage());
            } catch (Exception ex) {
                // 存储异常等：受控描述（异常类名），不落异常原文
                addFailure(resp, fileId, "STORAGE_ERROR: " + ex.getClass().getSimpleName());
                log.warn("[cleanup][导出件({}) 清理失败: {}]", fileId, ex.getClass().getName());
            }
        }
        log.info("[cleanup][导出件保留期清理：请求 {} 个，成功 {} 个，失败 {} 个]",
                fileIds.size(), resp.getSuccessIds().size(), resp.getFailures().size());
        return resp;
    }

    // ========== 内部方法 ==========

    private FileExportRetentionItemRespVO toItem(FileDO file) {
        FileExportRetentionItemRespVO item = new FileExportRetentionItemRespVO();
        item.setFileId(file.getId());
        item.setName(file.getName());
        item.setPurpose(file.getPurpose());
        item.setRetentionExpireTime(file.getRetentionExpireTime());
        return item;
    }

    private void addFailure(FileExportRetentionCleanupRespVO resp, Long fileId, String message) {
        FileExportRetentionItemRespVO failure = new FileExportRetentionItemRespVO();
        failure.setFileId(fileId);
        failure.setErrorMessage(message);
        resp.getFailures().add(failure);
    }

}
