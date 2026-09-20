package cn.zszj.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.framework.common.util.validation.ValidationUtils;
import cn.zszj.framework.datapermission.core.util.DataPermissionUtils;
import cn.zszj.module.infra.api.config.ConfigApi;
import cn.zszj.module.system.controller.admin.auth.vo.AuthRegisterReqVO;
import cn.zszj.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import cn.zszj.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import cn.zszj.module.system.controller.admin.user.vo.user.UserImportExcelVO;
import cn.zszj.module.system.controller.admin.user.vo.user.UserImportRespVO;
import cn.zszj.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.zszj.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.zszj.module.system.dal.dataobject.dept.DeptDO;
import cn.zszj.module.system.dal.dataobject.dept.UserPostDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.dept.DeptMapper;
import cn.zszj.module.system.dal.mysql.dept.UserPostMapper;
import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
import cn.zszj.module.system.mq.producer.user.AdminUserProducer;
import cn.zszj.module.system.service.dept.DeptService;
import cn.zszj.module.system.service.dept.PostService;
import cn.zszj.module.system.service.oauth2.OAuth2TokenService;
import cn.zszj.module.system.service.permission.PermissionService;
import cn.zszj.module.system.service.tenant.TenantService;
import com.google.common.annotations.VisibleForTesting;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.framework.common.util.collection.CollectionUtils.*;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;
import static cn.zszj.module.system.enums.LogRecordConstants.*;

/**
 * 后台用户 Service 实现类
 *
 * @author 芋道源码
 */
@Service("adminUserService")
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    static final String USER_INIT_PASSWORD_KEY = "system.user.init-password";

    static final String USER_REGISTER_ENABLED_KEY = "system.user.register-enabled";

    @Resource
    private AdminUserMapper userMapper;

    @Resource
    private DeptService deptService;
    @Resource
    private PostService postService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    @Lazy // 延迟，避免循环依赖报错
    private TenantService tenantService;
    @Resource
    @Lazy // 懒加载，避免循环依赖
    private OAuth2TokenService oauth2TokenService;

    @Resource
    private UserPostMapper userPostMapper;

    @Resource
    private DeptMapper deptMapper; // 负责人计数走 DeptMapper 原语即可，无需为一次计数在 DeptService 接口扩方法（本类已注入 DeptService）

    @Resource
    private ConfigApi configApi;

    @Resource
    private AdminUserProducer adminUserProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_CREATE_SUB_TYPE, bizNo = "{{#user.id}}",
            success = SYSTEM_USER_CREATE_SUCCESS)
    public Long createUser(UserSaveReqVO createReqVO) {
        // ZS-DB-010（D-09 M2）：账号规范化——trim + 小写化，使校验与入库均按规范化值
        createReqVO.setUsername(normalizeUsername(createReqVO.getUsername()));
        // 1.1 校验账户配合
        tenantService.handleTenantInfo(tenant -> {
            long count = userMapper.selectCount();
            if (count >= tenant.getAccountCount()) {
                throw exception(USER_COUNT_MAX, tenant.getAccountCount());
            }
        });
        // 1.2 校验正确性
        validateUserForCreateOrUpdate(null, createReqVO.getUsername(),
                createReqVO.getMobile(), createReqVO.getEmail(), createReqVO.getDeptId(), createReqVO.getPostIds());
        // 2.1 插入用户
        AdminUserDO user = BeanUtils.toBean(createReqVO, AdminUserDO.class);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus()); // 默认开启
        user.setPassword(encodePassword(createReqVO.getPassword())); // 加密密码
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            // ZS-DB-010（D-09 最终防线）：并发窗口下 DB 唯一约束冲突 → 稳定错误码
            throw translateDuplicateKey(ex);
        }
        // 2.2 插入关联岗位
        if (CollectionUtil.isNotEmpty(user.getPostIds())) {
            userPostMapper.insertBatch(convertList(user.getPostIds(),
                    postId -> new UserPostDO().setUserId(user.getId()).setPostId(postId)));
        }

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("user", user);
        return user.getId();
    }

    @Override
    public AdminUserDO registerUser(AuthRegisterReqVO registerReqVO) {
        // 1.1 校验是否开启注册
        if (ObjUtil.notEqual(configApi.getConfigValueByKey(USER_REGISTER_ENABLED_KEY), "true")) {
            throw exception(USER_REGISTER_DISABLED);
        }
        // 1.2 校验账户配合
        tenantService.handleTenantInfo(tenant -> {
            long count = userMapper.selectCount();
            if (count >= tenant.getAccountCount()) {
                throw exception(USER_COUNT_MAX, tenant.getAccountCount());
            }
        });
        // ZS-DB-010（D-09 M2）：注册入口同样规范化账号
        registerReqVO.setUsername(normalizeUsername(registerReqVO.getUsername()));
        // 1.3 校验正确性
        validateUserForCreateOrUpdate(null, registerReqVO.getUsername(), null, null, null, null);

        // 2. 插入用户
        AdminUserDO user = BeanUtils.toBean(registerReqVO, AdminUserDO.class);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus()); // 默认开启
        user.setPassword(encodePassword(registerReqVO.getPassword())); // 加密密码
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw translateDuplicateKey(ex);
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}",
            success = SYSTEM_USER_UPDATE_SUCCESS)
    public void updateUser(UserSaveReqVO updateReqVO) {
        updateReqVO.setPassword(null); // 特殊：此处不更新密码
        // ZS-DB-010（D-09 M2）：账号规范化（username 为空时原样，updateById 不覆盖）
        updateReqVO.setUsername(normalizeUsername(updateReqVO.getUsername()));
        // 1. 校验正确性
        AdminUserDO oldUser = validateUserForCreateOrUpdate(updateReqVO.getId(), updateReqVO.getUsername(),
                updateReqVO.getMobile(), updateReqVO.getEmail(), updateReqVO.getDeptId(), updateReqVO.getPostIds());

        // 2.1 更新用户
        AdminUserDO updateObj = BeanUtils.toBean(updateReqVO, AdminUserDO.class);
        try {
            userMapper.updateById(updateObj);
        } catch (DuplicateKeyException ex) {
            throw translateDuplicateKey(ex);
        }
        // 2.2 更新岗位
        updateUserPost(updateReqVO, updateObj);
        // 2.3 昵称 / 头像变化时，发送消息供下游订阅（如 IM 模块推 FRIEND_INFO_UPDATED）
        publishUserProfileUpdatedIfChanged(oldUser, updateReqVO.getNickname(), updateReqVO.getAvatar());

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtils.toBean(oldUser, UserSaveReqVO.class));
        LogRecordContext.putVariable("user", oldUser);
    }

    private void updateUserPost(UserSaveReqVO reqVO, AdminUserDO updateObj) {
        Long userId = reqVO.getId();
        Set<Long> dbPostIds = convertSet(userPostMapper.selectListByUserId(userId), UserPostDO::getPostId);
        // 计算新增和删除的岗位编号
        Set<Long> postIds = CollUtil.emptyIfNull(updateObj.getPostIds());
        Collection<Long> createPostIds = CollUtil.subtract(postIds, dbPostIds);
        Collection<Long> deletePostIds = CollUtil.subtract(dbPostIds, postIds);
        // 执行新增和删除。对于已经授权的岗位，不用做任何处理
        if (!CollectionUtil.isEmpty(createPostIds)) {
            userPostMapper.insertBatch(convertList(createPostIds,
                    postId -> new UserPostDO().setUserId(userId).setPostId(postId)));
        }
        if (!CollectionUtil.isEmpty(deletePostIds)) {
            userPostMapper.deleteByUserIdAndPostId(userId, deletePostIds);
        }
    }

    @Override
    public void updateUserLogin(Long id, String loginIp) {
        userMapper.updateById(new AdminUserDO().setId(id).setLoginIp(loginIp).setLoginDate(LocalDateTime.now()));
    }

    @Override
    public void updateUserProfile(Long id, UserProfileUpdateReqVO reqVO) {
        // 1. 校验正确性
        AdminUserDO oldUser = validateUserExists(id);
        validateEmailUnique(id, reqVO.getEmail());
        validateMobileUnique(id, reqVO.getMobile());

        // 2. 执行更新
        // ZS-DB-010 codex r1 P2：mobile/email 亦受全局唯一约束——跨租户或并发下 validateXxxUnique
        // 的租户内预校验放行后，DB 约束为最终防线；冲突须转稳定错误码，不得抛裸 DuplicateKeyException
        try {
            userMapper.updateById(BeanUtils.toBean(reqVO, AdminUserDO.class).setId(id));
        } catch (DuplicateKeyException ex) {
            throw translateDuplicateKey(ex);
        }

        // 3. 昵称 / 头像变化时，发送消息供下游订阅（如 IM 模块推 FRIEND_INFO_UPDATED）
        publishUserProfileUpdatedIfChanged(oldUser, reqVO.getNickname(), reqVO.getAvatar());
    }

    /**
     * 仅当 nickname 或 avatar 跟旧值不一致时，发送 AdminUserProfileUpdateMessage
     */
    private void publishUserProfileUpdatedIfChanged(AdminUserDO oldUser, String newNickname, String newAvatar) {
        boolean nicknameChanged = newNickname != null && !ObjUtil.equal(oldUser.getNickname(), newNickname);
        boolean avatarChanged = newAvatar != null && !ObjUtil.equal(oldUser.getAvatar(), newAvatar);
        if (!nicknameChanged && !avatarChanged) {
            return;
        }
        adminUserProducer.sendUserProfileUpdateMessage(oldUser.getId(),
                nicknameChanged ? newNickname : null,
                avatarChanged ? newAvatar : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_UPDATE_SELF_PASSWORD_SUB_TYPE, bizNo = "{{#id}}",
            success = SYSTEM_USER_UPDATE_SELF_PASSWORD_SUCCESS)
    public void updateUserPassword(Long id, UserProfileUpdatePasswordReqVO reqVO) {
        // 1. 校验用户存在、旧密码密码
        AdminUserDO user = validateUserExists(id);
        validateOldPassword(id, reqVO.getOldPassword());

        // 2. 执行更新
        AdminUserDO updateObj = new AdminUserDO().setId(id);
        updateObj.setPassword(encodePassword(reqVO.getNewPassword())); // 加密密码
        userMapper.updateById(updateObj);

        // 3. ZS-LOGIN-003：改密后全端失效（策略与理由见 invalidateUserSessions）
        invalidateUserSessions(id, "用户自助修改密码");

        // 4. 记录操作日志上下文
        LogRecordContext.putVariable("user", user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_UPDATE_PASSWORD_SUB_TYPE, bizNo = "{{#id}}",
            success = SYSTEM_USER_UPDATE_PASSWORD_SUCCESS)
    public void updateUserPassword(Long id, String password) {
        // 1. 校验用户存在
        AdminUserDO user = validateUserExists(id);

        // 2. 更新密码
        AdminUserDO updateObj = new AdminUserDO();
        updateObj.setId(id);
        updateObj.setPassword(encodePassword(password)); // 加密密码
        userMapper.updateById(updateObj);

        // 3. ZS-LOGIN-003：管理员重置他人密码后全端失效（重置通常意味着旧密码已泄露或人员变动）
        invalidateUserSessions(id, "管理员重置密码");

        // 4. 记录操作日志上下文
        LogRecordContext.putVariable("user", user);
        LogRecordContext.putVariable("newPassword", updateObj.getPassword());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_UPDATE_STATUS_SUB_TYPE, bizNo = "{{#id}}",
            success = SYSTEM_USER_UPDATE_STATUS_SUCCESS)
    public void updateUserStatus(Long id, Integer status) {
        // 1. 校验用户存在
        AdminUserDO user = validateUserExists(id);

        // 2. 更新状态
        AdminUserDO updateObj = new AdminUserDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        userMapper.updateById(updateObj);

        // 3. ZS-LOGIN-003：如果是禁用用户，则失效其全部登录会话
        if (CommonStatusEnum.isDisable(status)) {
            invalidateUserSessions(id, "禁用用户");
        }

        // 4. 记录操作日志上下文
        LogRecordContext.putVariable("user", user);
    }

    /**
     * ZS-LOGIN-003：失效指定用户的<b>全部</b>登录会话（access / refresh / 已缓存转换凭据 / 会话代际键）。
     *
     * <p><b>策略：全端失效，不保留「当前端」。</b>理由：
     * <ol>
     *     <li>{@code LoginUser} 与 {@code OAuth2AccessTokenCheckRespDTO} 均<b>不携带</b>访问令牌串，
     *         {@code TokenAuthenticationFilter} 也不把它写入安全上下文 —— 服务端当前<b>无法识别「当前端」</b>；</li>
     *     <li>任何由客户端上送（请求体 / 请求头 / URL 参数）的「保留哪一端」提示都<b>可被伪造</b>：
     *         攻击者持失窃凭据改密后可指定保留自己的会话，使改密彻底失去止损意义；</li>
     *     <li>{@link #updateUserPassword(Long, String)} 是管理员重置<b>他人</b>密码，
     *         根本不存在「目标用户的当前端」这个概念；</li>
     *     <li>改密 / 禁用 / 删除 本身即「怀疑凭据泄露或账号不再可用」的止损动作，全端失效是安全默认。</li>
     * </ol>
     * 代价：自助改密后需全端重新登录（下一次请求 401 → 前端跳登录页，属既有契约）。
     * 若产品后续要「保留当前端」，必须先在 {@code TokenAuthenticationFilter} 把<b>已验证的</b>
     * 访问令牌串写入 {@code LoginUser}（服务端来源、非客户端断言）—— 本任务<b>不预埋</b>。
     *
     * <p>撤销能力完全复用 {@link OAuth2TokenService#removeAccessToken(Long, Integer)}
     * （ZS-LOGIN-002 的行锁 + 固定锁序 + 会话代际键，ZS-LOGIN-003 的孤立刷新凭据全集 +
     * 已缓存转换凭据清理 + 忽略租户），<b>不</b>另造锁、<b>不</b>另造审计机制。
     *
     * <p>调用方<b>必须</b>处于 {@code @Transactional} 中：撤销失败则回滚状态变更 ——
     * 宁可显式失败，也不留下「已禁用 / 已删除 / 已改密但仍在线」的窗口。
     *
     * @param userId 用户编号
     * @param reason 失效原因（仅用于审计日志，与 {@code @LogRecord} 操作日志经 trace-id 关联；不入库、不入令牌）
     */
    private void invalidateUserSessions(Long userId, String reason) {
        oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
        // ZS-LOGIN-003：事件与审计可追踪。此处只记「哪个用户、因为什么」；
        // 撤销明细（会话数 / 其中孤立刷新凭据数）由 OAuth2TokenServiceImpl 侧的同 trace 日志给出。
        log.info("[invalidateUserSessions][ZS-LOGIN-003 用户({}) 的全部登录会话已失效，原因({})]", userId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_DELETE_SUB_TYPE, bizNo = "{{#id}}",
            success = SYSTEM_USER_DELETE_SUCCESS)
    public void deleteUser(Long id) {
        // 1. 校验用户存在
        AdminUserDO user = validateUserExists(id);
        // 1.1 校验用户不是部门负责人
        validateUserNotDeptLeader(id);

        // 2.1 删除用户
        userMapper.deleteById(id);
        // 2.2 删除用户关联数据
        permissionService.processUserDeleted(id);
        // 2.2 删除用户岗位
        userPostMapper.deleteByUserId(id);
        // 2.3 ZS-LOGIN-003：删除后失效全部登录会话（此前缺失：被删用户的凭据在自然过期前仍可用，
        // 因为 checkAccessToken 只校验令牌存在与到期，不校验用户是否仍存在）
        invalidateUserSessions(id, "删除用户");

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("user", user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserList(List<Long> ids) {
        // 1. 校验用户都存在、且在调用方租户可见范围内；与单条删除保持一致，且先完成全部校验再删除，避免部分删除
        // ZS-LOGIN-003 codex r0 P1：此前只校验「不是部门负责人」，不校验归属 —— 跨租户强制下线漏洞，详见 validateUsersExists
        validateUsersExists(ids);
        // 1.1 校验用户都不是部门负责人
        ids.forEach(this::validateUserNotDeptLeader);

        // 2.1 批量删除用户
        userMapper.deleteByIds(ids);

        // 2.2 批量删除用户关联数据
        ids.forEach(id -> {
            permissionService.processUserDeleted(id);
            userPostMapper.deleteByUserId(id);
            // 2.3 ZS-LOGIN-003：与单条删除对齐，批量删除同样必须失效会话
            invalidateUserSessions(id, "批量删除用户");
        });
    }

    @Override
    public AdminUserDO getUserByUsername(String username) {
        // ZS-DB-010（D-09 M2）：登录名按规范化值查询，兼容存量小写化
        return userMapper.selectByUsername(normalizeUsername(username));
    }

    @Override
    public AdminUserDO getUserByMobile(String mobile) {
        return userMapper.selectByMobile(mobile);
    }

    @Override
    public PageResult<AdminUserDO> getUserPage(UserPageReqVO reqVO) {
        // 如果有角色编号，查询角色对应的用户编号
        Set<Long> userIds = null;
        if (reqVO.getRoleId() != null) {
            userIds = permissionService.getUserRoleIdListByRoleId(singleton(reqVO.getRoleId()));
            if (CollUtil.isEmpty(userIds)) {
                return PageResult.empty();
            }
        }

        // 分页查询
        return userMapper.selectPage(reqVO, getDeptCondition(reqVO.getDeptId()), userIds);
    }

    @Override
    public AdminUserDO getUser(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public List<AdminUserDO> getUserListByDeptIds(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyList();
        }
        return userMapper.selectListByDeptIds(deptIds);
    }

    @Override
    public List<AdminUserDO> getUserListByPostIds(Collection<Long> postIds) {
        if (CollUtil.isEmpty(postIds)) {
            return Collections.emptyList();
        }
        Set<Long> userIds = convertSet(userPostMapper.selectListByPostIds(postIds), UserPostDO::getUserId);
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return userMapper.selectByIds(userIds);
    }

    @Override
    public List<AdminUserDO> getUserList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return userMapper.selectByIds(ids);
    }

    public List<AdminUserDO> getUserListAll() {
        return userMapper.selectList();
    }


    @Override
    public void validateUserList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // 获得岗位信息
        List<AdminUserDO> users = userMapper.selectByIds(ids);
        Map<Long, AdminUserDO> userMap = CollectionUtils.convertMap(users, AdminUserDO::getId);
        // 校验
        ids.forEach(id -> {
            AdminUserDO user = userMap.get(id);
            if (user == null) {
                throw exception(USER_NOT_EXISTS);
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus())) {
                throw exception(USER_IS_DISABLE, user.getNickname());
            }
        });
    }

    @Override
    public List<AdminUserDO> getUserListByNickname(String nickname) {
        return userMapper.selectListByNickname(nickname);
    }

    /**
     * 获得部门条件：查询指定部门的子部门编号们，包括自身
     *
     * @param deptId 部门编号
     * @return 部门编号集合
     */
    private Set<Long> getDeptCondition(Long deptId) {
        if (deptId == null) {
            return Collections.emptySet();
        }
        Set<Long> deptIds = convertSet(deptService.getChildDeptList(deptId), DeptDO::getId);
        deptIds.add(deptId); // 包括自身
        return deptIds;
    }

    private AdminUserDO validateUserForCreateOrUpdate(Long id, String username, String mobile, String email,
                                               Long deptId, Set<Long> postIds) {
        // 关闭数据权限，避免因为没有数据权限，查询不到数据，进而导致唯一校验不正确
        return DataPermissionUtils.executeIgnore(() -> {
            // 校验用户存在
            AdminUserDO user = validateUserExists(id);
            // 校验用户名唯一
            validateUsernameUnique(id, username);
            // 校验手机号唯一
            validateMobileUnique(id, mobile);
            // 校验邮箱唯一
            validateEmailUnique(id, email);
            // 校验部门处于开启状态
            deptService.validateDeptList(CollectionUtils.singleton(deptId));
            // 校验岗位处于开启状态
            postService.validatePostList(postIds);
            return user;
        });
    }

    @VisibleForTesting
    AdminUserDO validateUserExists(Long id) {
        if (id == null) {
            return null;
        }
        AdminUserDO user = userMapper.selectById(id);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        return user;
    }

    /**
     * ZS-LOGIN-003（codex r0 P1 修复）：批量校验用户都存在，且在<b>调用方租户可见范围内</b>。
     *
     * <p><b>为何必须补</b>：{@link #deleteUserList(List)} 此前只校验「不是部门负责人」。
     * {@code userMapper.deleteByIds} 受租户拦截器过滤（他租户账号根本删不掉），
     * 但会话撤销走 {@link #invalidateUserSessions} → {@code OAuth2TokenService#removeAccessToken(Long, Integer)}，
     * 后者为了「不被调用方租户上下文静默收窄」而在 {@code TenantUtils.executeIgnore} 作用域内执行、是<b>全局</b>的。
     * 两者叠加就形成漏洞：持有 {@code system:user:delete} 的管理员只要向他租户用户编号，
     * 就能在账号<b>未被删除</b>的情况下把对方<b>跨租户强制下线</b>（DoS）。
     *
     * <p>因此本校验与单条删除的 {@link #validateUserExists(Long)} 对齐：任一编号不可见即<b>整批失败</b>，
     * 对外统一抛 {@code USER_NOT_EXISTS}（他租户编号与真不存在不可区分，不泄露跨租户存在性）。
     *
     * <p><b>关键：本校验必须留在调用方租户作用域内执行（绝不得包进 {@code executeIgnore}）</b>，
     * 否则租户过滤失效、他租户编号也能通过校验，修复即形同虚设。
     * 只有校验通过后的撤销才允许进入忽略租户作用域。
     *
     * @param ids 用户编号集合
     */
    private void validateUsersExists(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // 去重后比较数量：selectByIds 同样受「租户过滤 + 逻辑删除过滤」，不可见的编号不会返回
        Set<Long> distinctIds = new HashSet<>(ids);
        if (CollUtil.size(userMapper.selectByIds(distinctIds)) != distinctIds.size()) {
            throw exception(USER_NOT_EXISTS);
        }
    }

    /**
     * 校验用户不是部门负责人，避免删除后 {@link DeptDO#getLeaderUserId()} 悬空
     *
     * 说明：此处只做技术层的引用保护，要求先变更负责人再删除用户，不自动级联抹除负责人历史；
     * 完整的任职生命周期与责任历史归属迁移，待 D-09 任职模型后由 ZS-IAM-002/004 承接
     *
     * @param id 用户编号
     */
    @VisibleForTesting
    void validateUserNotDeptLeader(Long id) {
        // 关闭数据权限，避免调用者数据范围外的部门（该用户担任负责人）被过滤掉，
        // 导致负责人引用保护漏判、删除用户后 DeptDO.leaderUserId 悬空；租户过滤仍保留
        Long leaderDeptCount = DataPermissionUtils.executeIgnore(() -> deptMapper.selectCountByLeaderUserId(id));
        if (leaderDeptCount > 0) {
            throw exception(USER_IS_DEPT_LEADER);
        }
    }

    /**
     * ZS-DB-010（D-09 M2）：账号规范化——trim + 小写化，保证登录名全平台唯一按规范化值判定。
     * blank/null 原样返回，避免破坏可选字段语义。
     */
    @VisibleForTesting
    String normalizeUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return username;
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * PG 唯一约束冲突报错中约束名的提取模式：形如 ... violates unique constraint "uk_xxx" ...。
     * codex r2 P2：DETAIL 里回显的冲突值用圆括号包裹（Key (email)=(...)），不会落入本模式的双引号捕获组，
     * 故按约束名精确比对可杜绝「邮箱值恰含 uk_system_users_mobile 子串」这类误判。
     */
    private static final Pattern DUPLICATE_KEY_CONSTRAINT_PATTERN = Pattern.compile("unique constraint \"([^\"]+)\"");

    /**
     * ZS-DB-010（D-09 最终防线）：将数据库唯一约束冲突（PG 23505 → Spring {@link DuplicateKeyException}）
     * 映射为稳定业务错误码。应用层 validateXxxUnique 存在并发窗口，DB 约束为最终防线。
     * 按约束名分派；无法辨识时保守归为用户账号冲突并记 WARN，绝不吞异常或回退成功。
     */
    private ServiceException translateDuplicateKey(DuplicateKeyException ex) {
        String message = String.valueOf(ex.getMessage());
        // codex r2 P2：精确提取真实约束名再比对，避免 PG DETAIL 回显的冲突值（如邮箱恰含 uk_system_users_mobile）造成子串误判
        String constraint = extractConstraintName(message);
        if ("uk_system_users_mobile".equals(constraint)) {
            return exception(USER_MOBILE_EXISTS);
        }
        if ("uk_system_users_email".equals(constraint)) {
            return exception(USER_EMAIL_EXISTS);
        }
        if ("uk_system_users_username".equals(constraint)) {
            return exception(USER_USERNAME_EXISTS);
        }
        log.warn("[translateDuplicateKey][ZS-DB-010 未辨识的唯一约束冲突，保守归为账号冲突 constraint={} message={}]", constraint, message);
        return exception(USER_USERNAME_EXISTS);
    }

    /** 从 PG 冲突报错中提取真实约束名（首个 unique constraint "..." 捕获组）；无法提取时返回空串走保守分支。 */
    private String extractConstraintName(String message) {
        Matcher matcher = DUPLICATE_KEY_CONSTRAINT_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : "";
    }

    @VisibleForTesting
    void validateUsernameUnique(Long id, String username) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        AdminUserDO user = userMapper.selectByUsername(username);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw exception(USER_USERNAME_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_USERNAME_EXISTS);
        }
    }

    @VisibleForTesting
    void validateEmailUnique(Long id, String email) {
        if (StrUtil.isBlank(email)) {
            return;
        }
        AdminUserDO user = userMapper.selectByEmail(email);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw exception(USER_EMAIL_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_EMAIL_EXISTS);
        }
    }

    @VisibleForTesting
    void validateMobileUnique(Long id, String mobile) {
        if (StrUtil.isBlank(mobile)) {
            return;
        }
        AdminUserDO user = userMapper.selectByMobile(mobile);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw exception(USER_MOBILE_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_MOBILE_EXISTS);
        }
    }

    /**
     * 校验旧密码
     * @param id          用户 id
     * @param oldPassword 旧密码
     */
    @VisibleForTesting
    void validateOldPassword(Long id, String oldPassword) {
        AdminUserDO user = userMapper.selectById(id);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        if (!isPasswordMatch(oldPassword, user.getPassword())) {
            throw exception(USER_PASSWORD_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 添加事务，异常则回滚所有导入
    public UserImportRespVO importUserList(List<UserImportExcelVO> importUsers, boolean isUpdateSupport) {
        // 1.1 参数校验
        if (CollUtil.isEmpty(importUsers)) {
            throw exception(USER_IMPORT_LIST_IS_EMPTY);
        }
        // 1.2 初始化密码不能为空
        String initPassword = configApi.getConfigValueByKey(USER_INIT_PASSWORD_KEY);
        if (StrUtil.isEmpty(initPassword)) {
            throw exception(USER_IMPORT_INIT_PASSWORD);
        }

        // 2. 遍历，逐个创建 or 更新
        UserImportRespVO respVO = UserImportRespVO.builder().createUsernames(new ArrayList<>())
                .updateUsernames(new ArrayList<>()).failureUsernames(new LinkedHashMap<>()).build();
        AtomicInteger index = new AtomicInteger(1);
        importUsers.forEach(importUser -> {
            int currentIndex = index.getAndIncrement();
            // 2.1.1 校验字段是否符合要求
            try {
                ValidationUtils.validate(BeanUtils.toBean(importUser, UserSaveReqVO.class).setPassword(initPassword));
            } catch (ConstraintViolationException ex) {
                String key = StrUtil.blankToDefault(importUser.getUsername(), "第 " + currentIndex + " 行");
                respVO.getFailureUsernames().put(key, ex.getMessage());
                return;
            }
            // 2.1.2 校验，判断是否有不符合的原因
            try {
                validateUserForCreateOrUpdate(null, null, importUser.getMobile(), importUser.getEmail(),
                        importUser.getDeptId(), null);
            } catch (ServiceException ex) {
                respVO.getFailureUsernames().put(importUser.getUsername(), ex.getMessage());
                return;
            }

            // 2.2.1 判断如果不存在，在进行插入
            // ZS-DB-010（D-09 M2）：导入账号规范化——查询与入库均按 trim + 小写值
            String normalizedUsername = normalizeUsername(importUser.getUsername());
            AdminUserDO existUser = userMapper.selectByUsername(normalizedUsername);
            if (existUser == null) {
                try {
                    userMapper.insert(BeanUtils.toBean(importUser, AdminUserDO.class)
                            .setUsername(normalizedUsername)
                            .setPassword(encodePassword(initPassword)).setPostIds(new HashSet<>())); // 设置默认密码及空岗位编号数组
                } catch (DuplicateKeyException ex) {
                    // ZS-DB-010 codex r1 P1：撞唯一约束后 PG 事务已 abort（SQLSTATE 25P02），同事务内后续行的
                    // selectByUsername/insert 全部失败、且此前成功行会被回滚。若 catch 后继续循环，会产出
                    // 「已回滚却报成功」的假台账。故 rethrow 让整批失败回滚（与本方法 rollbackFor=Exception 语义一致）。
                    throw translateDuplicateKey(ex);
                }
                respVO.getCreateUsernames().add(importUser.getUsername());
                return;
            }
            // 2.2.2 如果存在，判断是否允许更新
            if (!isUpdateSupport) {
                respVO.getFailureUsernames().put(importUser.getUsername(), USER_USERNAME_EXISTS.getMsg());
                return;
            }
            AdminUserDO updateUser = BeanUtils.toBean(importUser, AdminUserDO.class);
            updateUser.setId(existUser.getId());
            updateUser.setUsername(normalizedUsername);
            try {
                userMapper.updateById(updateUser);
            } catch (DuplicateKeyException ex) {
                // ZS-DB-010 codex r1 P1：同上——事务已 abort，不可继续；rethrow 让整批回滚
                throw translateDuplicateKey(ex);
            }
            // ZS-LOGIN-003 codex r1 P1 + codex r2 P1：覆盖导入把账号置为禁用即撤销全部会话——
            // 该入口此前未接统一撤销；且 existUser.status 为未加锁读，依「旧状态=已禁用」跳过撤销
            // 会漏掉「导入读旧快照 → 他人启用并登录 → 导入写禁用」交错留下的存活会话，
            // 故与 updateUserStatus 的防御性语义对齐：只要本次写入禁用状态就撤销（幂等、开销可忽略）
            if (CommonStatusEnum.isDisable(updateUser.getStatus())) {
                invalidateUserSessions(existUser.getId(), "Excel 覆盖导入禁用");
            }
            respVO.getUpdateUsernames().add(importUser.getUsername());
        });
        return respVO;
    }

    @Override
    public List<AdminUserDO> getUserListByStatus(Integer status) {
        return getUserListByStatus(status, null);
    }

    @Override
    public List<AdminUserDO> getUserListByStatus(Integer status, Long deptId) {
        return userMapper.selectListByStatusAndDeptId(status, deptId);
    }

    @Override
    public boolean isPasswordMatch(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 对密码进行加密
     *
     * @param password 密码
     * @return 加密后的密码
     */
    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

}
