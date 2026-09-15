package cn.zszj.module.infra.dal.mysql.file;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.infra.controller.admin.file.vo.file.FilePageReqVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
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

}
