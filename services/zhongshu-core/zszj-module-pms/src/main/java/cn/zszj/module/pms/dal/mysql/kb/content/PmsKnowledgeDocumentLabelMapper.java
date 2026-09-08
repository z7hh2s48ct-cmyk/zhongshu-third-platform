package cn.zszj.module.pms.dal.mysql.kb.content;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.module.pms.dal.dataobject.kb.content.PmsKnowledgeDocumentLabelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PmsKnowledgeDocumentLabelMapper extends BaseMapperX<PmsKnowledgeDocumentLabelDO> {

    default List<PmsKnowledgeDocumentLabelDO> selectList() {
        return selectList(new LambdaQueryWrapperX<PmsKnowledgeDocumentLabelDO>()
                .orderByAsc(PmsKnowledgeDocumentLabelDO::getCreateTime)
                .orderByAsc(PmsKnowledgeDocumentLabelDO::getId));
    }

}
