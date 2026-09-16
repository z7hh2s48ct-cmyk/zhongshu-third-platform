package cn.zszj.module.infra.dal.mysql.file;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.infra.dal.dataobject.file.FileUploadCredentialDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预签名直传凭证 Mapper（ZS-FILE-003）
 *
 * @author ZS-FILE-003
 */
@Mapper
public interface FileUploadCredentialMapper extends BaseMapperX<FileUploadCredentialDO> {

    default FileUploadCredentialDO selectByToken(String token) {
        return selectOne(FileUploadCredentialDO::getCredentialToken, token);
    }

    /**
     * ZS-FILE-003：完成确认的一次性原子迁移——仅当仍处 WAITING_UPLOAD 时迁移为 COMPLETED，
     * 并发/重复确认只有一个成功（affected=1）。
     */
    default int updateStatusToCompletedIfWaiting(Long id, Long fileId) {
        FileUploadCredentialDO updateObj = new FileUploadCredentialDO();
        updateObj.setId(id);
        updateObj.setStatus(FileUploadCredentialDO.STATUS_COMPLETED);
        updateObj.setFileId(fileId);
        updateObj.setCompletedTime(java.time.LocalDateTime.now());
        LambdaQueryWrapperX<FileUploadCredentialDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(FileUploadCredentialDO::getId, id);
        wrapper.eq(FileUploadCredentialDO::getStatus, FileUploadCredentialDO.STATUS_WAITING_UPLOAD);
        return update(updateObj, wrapper);
    }

    /**
     * 活跃凭证统计（ZS-FILE-005.B 孤儿核验）：仍在等待上传且未过期的凭证数——
     * temp/ 对象存在活跃凭证认领即视为在途直传，不得按孤儿清理。
     * 调用方须以 TenantUtils.executeIgnore 包裹做跨租户全局核验（任意租户在途上传都不可误删）。
     * codex r0 P1-2：temp_path 按 LOWER 两侧折叠（大小写不敏感文件系统下清点拼写与凭证拼写可能不同）。
     */
    default Long selectCountActiveByTempPathIgnoreTenant(String tempPath, java.time.LocalDateTime now) {
        return selectCount(new LambdaQueryWrapperX<FileUploadCredentialDO>()
                .apply("LOWER(temp_path) = LOWER({0})", tempPath)
                .eq(FileUploadCredentialDO::getStatus, FileUploadCredentialDO.STATUS_WAITING_UPLOAD)
                .gt(FileUploadCredentialDO::getExpiresTime, now));
    }

}
