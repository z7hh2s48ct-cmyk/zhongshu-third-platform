package cn.zszj.module.infra.dal.mysql.file;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.infra.dal.dataobject.file.FileDeliveryTicketDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件交付票据 Mapper（ZS-FILE-004.A）
 */
@Mapper
public interface FileDeliveryTicketMapper extends BaseMapperX<FileDeliveryTicketDO> {

    default FileDeliveryTicketDO selectByTicketHash(String ticketHash) {
        return selectOne(FileDeliveryTicketDO::getTicketHash, ticketHash);
    }

    default FileDeliveryTicketDO selectByDeliverySessionId(String deliverySessionId) {
        return selectOne(FileDeliveryTicketDO::getDeliverySessionId, deliverySessionId);
    }

    /**
     * 文件进行中的交付会话（REDEEMED 且未过期）——FILE-005.A 引用保护判定使用。
     */
    default List<FileDeliveryTicketDO> selectActiveRedeemedByFileId(Long fileId, LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<FileDeliveryTicketDO>()
                .eq(FileDeliveryTicketDO::getFileId, fileId)
                .eq(FileDeliveryTicketDO::getStatus, FileDeliveryTicketDO.STATUS_REDEEMED)
                .gt(FileDeliveryTicketDO::getExpiresTime, now));
    }

    default List<FileDeliveryTicketDO> selectListByStatus(String status) {
        return selectList(new LambdaQueryWrapperX<FileDeliveryTicketDO>()
                .eq(FileDeliveryTicketDO::getStatus, status)
                .orderByDesc(FileDeliveryTicketDO::getId));
    }

}
