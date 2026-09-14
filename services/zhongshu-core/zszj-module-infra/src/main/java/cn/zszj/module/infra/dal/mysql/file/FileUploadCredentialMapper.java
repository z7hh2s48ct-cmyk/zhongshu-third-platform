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

}
