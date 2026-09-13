package cn.zszj.module.system.service.dept;

import cn.hutool.core.collection.CollUtil;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.module.system.controller.admin.dept.vo.post.PostPageReqVO;
import cn.zszj.module.system.controller.admin.dept.vo.post.PostSaveReqVO;
import cn.zszj.framework.datapermission.core.util.DataPermissionUtils;
import cn.zszj.module.system.dal.dataobject.dept.PostDO;
import cn.zszj.module.system.dal.mysql.dept.PostMapper;
import cn.zszj.module.system.dal.mysql.dept.UserPostMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;

/**
 * 岗位 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class PostServiceImpl implements PostService {

    @Resource
    private PostMapper postMapper;

    @Resource
    private UserPostMapper userPostMapper;

    @Override
    public Long createPost(PostSaveReqVO createReqVO) {
        // 校验正确性
        validatePostForCreateOrUpdate(null, createReqVO.getName(), createReqVO.getCode());

        // 插入岗位
        PostDO post = BeanUtils.toBean(createReqVO, PostDO.class);
        postMapper.insert(post);
        return post.getId();
    }

    @Override
    public void updatePost(PostSaveReqVO updateReqVO) {
        // 校验正确性
        validatePostForCreateOrUpdate(updateReqVO.getId(), updateReqVO.getName(), updateReqVO.getCode());

        // 更新岗位
        PostDO updateObj = BeanUtils.toBean(updateReqVO, PostDO.class);
        postMapper.updateById(updateObj);
    }

    @Override
    public void deletePost(Long id) {
        // 校验是否存在
        validatePostExists(id);
        // ZS-IAM-003（ZS-SYS-001.A 真实 PG 回归发现）：岗位被用户引用时禁止删除；
        // 引用计数须关闭部门数据权限过滤（IAM-003 r0 教训：计数必须全量）、保留租户过滤
        validatePostNoUserReference(id);
        // 删除岗位
        postMapper.deleteById(id);
    }

    @Override
    public void deletePostList(List<Long> ids) {
        // 与单条删除保持一致：先完成全部引用校验再删除，避免部分删除
        ids.forEach(this::validatePostNoUserReference);
        postMapper.deleteByIds(ids);
    }

    private void validatePostNoUserReference(Long id) {
        Long count = DataPermissionUtils.executeIgnore(() -> userPostMapper.selectCountByPostId(id));
        if (count > 0) {
            throw exception(POST_EXITS_USERS);
        }
    }

    private void validatePostForCreateOrUpdate(Long id, String name, String code) {
        // 校验自己存在
        validatePostExists(id);
        // 校验岗位名的唯一性
        validatePostNameUnique(id, name);
        // 校验岗位编码的唯一性
        validatePostCodeUnique(id, code);
    }

    private void validatePostNameUnique(Long id, String name) {
        PostDO post = postMapper.selectByName(name);
        if (post == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的岗位
        if (id == null) {
            throw exception(POST_NAME_DUPLICATE);
        }
        if (!post.getId().equals(id)) {
            throw exception(POST_NAME_DUPLICATE);
        }
    }

    private void validatePostCodeUnique(Long id, String code) {
        PostDO post = postMapper.selectByCode(code);
        if (post == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的岗位
        if (id == null) {
            throw exception(POST_CODE_DUPLICATE);
        }
        if (!post.getId().equals(id)) {
            throw exception(POST_CODE_DUPLICATE);
        }
    }

    private void validatePostExists(Long id) {
        if (id == null) {
            return;
        }
        if (postMapper.selectById(id) == null) {
            throw exception(POST_NOT_FOUND);
        }
    }

    @Override
    public List<PostDO> getPostList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return postMapper.selectByIds(ids);
    }

    @Override
    public List<PostDO> getPostList(Collection<Long> ids, Collection<Integer> statuses) {
        return postMapper.selectList(ids, statuses);
    }

    @Override
    public PageResult<PostDO> getPostPage(PostPageReqVO reqVO) {
        return postMapper.selectPage(reqVO);
    }

    @Override
    public PostDO getPost(Long id) {
        return postMapper.selectById(id);
    }

    @Override
    public void validatePostList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // 获得岗位信息
        List<PostDO> posts = postMapper.selectByIds(ids);
        Map<Long, PostDO> postMap = convertMap(posts, PostDO::getId);
        // 校验
        ids.forEach(id -> {
            PostDO post = postMap.get(id);
            if (post == null) {
                throw exception(POST_NOT_FOUND);
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(post.getStatus())) {
                throw exception(POST_NOT_ENABLE, post.getName());
            }
        });
    }
}
