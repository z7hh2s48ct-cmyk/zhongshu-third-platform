package cn.zszj.module.bpm.service.task;

import cn.hutool.core.collection.ListUtil;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zszj.module.bpm.controller.admin.task.vo.task.BpmTaskDelegateReqVO;
import cn.zszj.module.bpm.controller.admin.task.vo.task.BpmTaskSignCreateReqVO;
import cn.zszj.module.bpm.controller.admin.task.vo.task.BpmTaskSignDeleteReqVO;
import cn.zszj.module.bpm.controller.admin.task.vo.task.BpmTaskTransferReqVO;
import cn.zszj.module.bpm.enums.task.BpmCommentTypeEnum;
import cn.zszj.module.bpm.enums.task.BpmTaskSignTypeEnum;
import cn.zszj.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.zszj.module.bpm.service.comment.BpmCommentService;
import cn.zszj.module.bpm.service.definition.BpmFormService;
import cn.zszj.module.bpm.service.definition.BpmModelService;
import cn.zszj.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.zszj.module.bpm.service.message.BpmMessageService;
import cn.zszj.module.system.api.dept.DeptApi;
import cn.zszj.module.system.api.user.AdminUserApi;
import cn.zszj.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.NativeTaskQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static cn.zszj.framework.common.util.collection.SetUtils.asSet;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link BpmTaskServiceImpl} 的统一审批资格与对象授权校验的单元测试
 *
 * 覆盖：validateTask 审批资格、delegate/transfer 目标用户禁用、createSignTask 加签用户校验、
 * 任务查询的租户条件隔离、getTaskListByParentTaskId 与 deleteSignTask 的对象授权
 *
 * @author zszj
 */
public class BpmTaskServiceImplSecurityTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BpmTaskServiceImpl bpmTaskService;

    @Mock
    private TaskService taskService;
    @Mock
    private HistoryService historyService;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private ManagementService managementService;
    @Mock
    private BpmProcessInstanceService processInstanceService;
    @Mock
    private BpmProcessDefinitionService bpmProcessDefinitionService;
    @Mock
    private BpmProcessInstanceCopyService processInstanceCopyService;
    @Mock
    private BpmCommentService commentService;
    @Mock
    private BpmModelService modelService;
    @Mock
    private BpmMessageService messageService;
    @Mock
    private BpmFormService formService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;

    // ========== validateTask 审批资格校验 ==========

    /**
     * 场景：任务审批人不是本人，校验失败
     */
    @Test
    public void testValidateTask_assigneeNotSelf_throw() {
        // mock 数据
        Task task = mock(Task.class);
        when(task.getAssignee()).thenReturn("2");
        TaskQuery query = mock(TaskQuery.class);
        mockGetTaskQuery(query, "t1", task);
        when(taskService.createTaskQuery()).thenReturn(query);
        // 调用，并断言
        assertServiceException(() -> bpmTaskService.validateTask(1L, "t1"), TASK_OPERATE_FAIL_ASSIGN_NOT_SELF);
    }

    /**
     * 场景：任务没有审批人（自动审批通过场景），外部用户不允许操作
     */
    @Test
    public void testValidateTask_assigneeBlank_externalUser_throwNoAssignee() {
        // mock 数据
        Task task = mock(Task.class);
        TaskQuery query = mock(TaskQuery.class);
        mockGetTaskQuery(query, "t1", task);
        when(taskService.createTaskQuery()).thenReturn(query);
        // 调用，并断言
        assertServiceException(() -> bpmTaskService.validateTask(1L, "t1"), TASK_OPERATE_FAIL_NO_ASSIGNEE);
    }

    /**
     * 场景：任务没有审批人，内部调用（userId 为空）允许通过
     */
    @Test
    public void testValidateTask_assigneeBlank_internalUser_allowed() {
        // mock 数据
        Task task = mock(Task.class);
        TaskQuery query = mock(TaskQuery.class);
        mockGetTaskQuery(query, "t1", task);
        when(taskService.createTaskQuery()).thenReturn(query);
        // 调用，并断言
        assertSame(task, bpmTaskService.validateTask(null, "t1"));
    }

    /**
     * 场景：任务不存在，校验失败
     */
    @Test
    public void testValidateTask_notExists_throw() {
        // mock 数据
        TaskQuery query = mock(TaskQuery.class);
        mockGetTaskQuery(query, "t1", null);
        when(taskService.createTaskQuery()).thenReturn(query);
        // 调用，并断言
        assertServiceException(() -> bpmTaskService.validateTask(1L, "t1"), TASK_NOT_EXISTS);
    }

    // ========== delegateTask 委派目标用户校验 ==========

    /**
     * 场景：被委派人已被禁用，委派失败
     */
    @Test
    public void testDelegateTask_targetUserDisabled_throw() {
        // mock 数据
        Task task = mock(Task.class);
        when(task.getAssignee()).thenReturn("1");
        TaskQuery query = mock(TaskQuery.class);
        mockGetTaskQuery(query, "t1", task);
        when(taskService.createTaskQuery()).thenReturn(query);
        AdminUserRespDTO delegateUser = randomPojo(AdminUserRespDTO.class,
                user -> user.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        when(adminUserApi.getUser(2L)).thenReturn(delegateUser);
        // 调用，并断言
        BpmTaskDelegateReqVO reqVO = new BpmTaskDelegateReqVO();
        reqVO.setId("t1");
        reqVO.setDelegateUserId(2L);
        reqVO.setReason("委派原因");
        assertServiceException(() -> bpmTaskService.delegateTask(1L, reqVO), TASK_DELEGATE_FAIL_USER_DISABLED);
    }

    /**
     * 场景：被委派人不存在，委派失败
     */
    @Test
    public void testDelegateTask_targetUserNotExists_throw() {
        // mock 数据
        Task task = mock(Task.class);
        when(task.getAssignee()).thenReturn("1");
        TaskQuery query = mock(TaskQuery.class);
        mockGetTaskQuery(query, "t1", task);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(adminUserApi.getUser(2L)).thenReturn(null);
        // 调用，并断言
        BpmTaskDelegateReqVO reqVO = new BpmTaskDelegateReqVO();
        reqVO.setId("t1");
        reqVO.setDelegateUserId(2L);
        reqVO.setReason("委派原因");
        assertServiceException(() -> bpmTaskService.delegateTask(1L, reqVO), TASK_DELEGATE_FAIL_USER_NOT_EXISTS);
    }

    // ========== transferTask 转办目标用户校验 ==========

    /**
     * 场景：转办人已被禁用，转办失败
     */
    @Test
    public void testTransferTask_targetUserDisabled_throw() {
        // mock 数据
        Task task = mock(Task.class);
        when(task.getAssignee()).thenReturn("1");
        TaskQuery query = mock(TaskQuery.class);
        mockGetTaskQuery(query, "t1", task);
        when(taskService.createTaskQuery()).thenReturn(query);
        AdminUserRespDTO assigneeUser = randomPojo(AdminUserRespDTO.class,
                user -> user.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        when(adminUserApi.getUser(2L)).thenReturn(assigneeUser);
        // 调用，并断言
        BpmTaskTransferReqVO reqVO = new BpmTaskTransferReqVO();
        reqVO.setId("t1");
        reqVO.setAssigneeUserId(2L);
        reqVO.setReason("转办原因");
        assertServiceException(() -> bpmTaskService.transferTask(1L, reqVO), TASK_TRANSFER_FAIL_USER_DISABLED);
    }

    // ========== createSignTask 加签用户校验 ==========

    /**
     * 场景：加签用户部分不存在（返回数量少于请求数量），加签失败
     */
    @Test
    public void testCreateSignTask_usersIncomplete_throwNotExist() {
        // mock 数据
        TaskEntityImpl taskEntity = mock(TaskEntityImpl.class);
        when(taskEntity.getAssignee()).thenReturn("1");
        when(taskEntity.getProcessInstanceId()).thenReturn("p1");
        when(taskEntity.getTaskDefinitionKey()).thenReturn("key1");
        TaskQuery query = mock(TaskQuery.class);
        mockGetTaskQuery(query, "t1", taskEntity);
        TaskQuery taskListQuery = mock(TaskQuery.class);
        when(taskListQuery.processInstanceId("p1")).thenReturn(taskListQuery);
        when(taskListQuery.taskDefinitionKey("key1")).thenReturn(taskListQuery);
        when(taskListQuery.list()).thenReturn(Collections.emptyList());
        when(taskService.createTaskQuery()).thenReturn(query, taskListQuery);
        Set<Long> signUserIds = asSet(2L, 3L);
        AdminUserRespDTO user2 = randomPojo(AdminUserRespDTO.class);
        when(adminUserApi.getUserList(signUserIds)).thenReturn(ListUtil.of(user2));
        // 调用，并断言
        BpmTaskSignCreateReqVO reqVO = new BpmTaskSignCreateReqVO();
        reqVO.setId("t1");
        reqVO.setUserIds(signUserIds);
        reqVO.setType(BpmTaskSignTypeEnum.AFTER.getType());
        reqVO.setReason("加签原因");
        assertServiceException(() -> bpmTaskService.createSignTask(1L, reqVO), TASK_SIGN_CREATE_USER_NOT_EXIST);
    }

    /**
     * 场景：加签用户存在被禁用的用户，加签失败
     */
    @Test
    public void testCreateSignTask_userDisabled_throwDisabled() {
        // mock 数据
        TaskEntityImpl taskEntity = mock(TaskEntityImpl.class);
        when(taskEntity.getAssignee()).thenReturn("1");
        when(taskEntity.getProcessInstanceId()).thenReturn("p1");
        when(taskEntity.getTaskDefinitionKey()).thenReturn("key1");
        TaskQuery query = mock(TaskQuery.class);
        mockGetTaskQuery(query, "t1", taskEntity);
        TaskQuery taskListQuery = mock(TaskQuery.class);
        when(taskListQuery.processInstanceId("p1")).thenReturn(taskListQuery);
        when(taskListQuery.taskDefinitionKey("key1")).thenReturn(taskListQuery);
        when(taskListQuery.list()).thenReturn(Collections.emptyList());
        when(taskService.createTaskQuery()).thenReturn(query, taskListQuery);
        Set<Long> signUserIds = asSet(2L, 3L);
        AdminUserRespDTO user2 = randomPojo(AdminUserRespDTO.class,
                user -> user.setStatus(CommonStatusEnum.ENABLE.getStatus()));
        AdminUserRespDTO user3 = randomPojo(AdminUserRespDTO.class,
                user -> user.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        when(adminUserApi.getUserList(signUserIds)).thenReturn(ListUtil.of(user2, user3));
        // 调用，并断言
        BpmTaskSignCreateReqVO reqVO = new BpmTaskSignCreateReqVO();
        reqVO.setId("t1");
        reqVO.setUserIds(signUserIds);
        reqVO.setType(BpmTaskSignTypeEnum.AFTER.getType());
        reqVO.setReason("加签原因");
        assertServiceException(() -> bpmTaskService.createSignTask(1L, reqVO), TASK_SIGN_CREATE_USER_DISABLED);
    }

    // ========== 任务查询的租户条件隔离 ==========

    /**
     * 场景：存在租户上下文时，getTask 查询追加租户条件
     */
    @Test
    public void testGetTask_tenantContextPresent_addTenantCondition() {
        // mock 数据
        Task task = mock(Task.class);
        TaskQuery query = mock(TaskQuery.class);
        when(query.taskId("t1")).thenReturn(query);
        when(query.includeTaskLocalVariables()).thenReturn(query);
        when(query.taskTenantId("123")).thenReturn(query);
        when(query.singleResult()).thenReturn(task);
        when(taskService.createTaskQuery()).thenReturn(query);
        try (MockedStatic<FlowableUtils> flowableUtilsMocked = mockStatic(FlowableUtils.class)) {
            flowableUtilsMocked.when(FlowableUtils::getTenantId).thenReturn("123");
            // 调用，并断言
            assertSame(task, bpmTaskService.getTask("t1"));
            verify(query).taskTenantId("123");
        }
    }

    /**
     * 场景：无租户上下文时，getTask 查询不追加租户条件
     */
    @Test
    public void testGetTask_noTenantContext_skipTenantCondition() {
        // mock 数据
        Task task = mock(Task.class);
        TaskQuery query = mock(TaskQuery.class);
        when(query.taskId("t1")).thenReturn(query);
        when(query.includeTaskLocalVariables()).thenReturn(query);
        when(query.singleResult()).thenReturn(task);
        when(taskService.createTaskQuery()).thenReturn(query);
        try (MockedStatic<FlowableUtils> flowableUtilsMocked = mockStatic(FlowableUtils.class)) {
            flowableUtilsMocked.when(FlowableUtils::getTenantId).thenReturn(ProcessEngineConfiguration.NO_TENANT_ID);
            // 调用，并断言
            assertSame(task, bpmTaskService.getTask("t1"));
            verify(query, never()).taskTenantId(anyString());
        }
    }

    /**
     * 场景：存在租户上下文时，getHistoricTask 查询追加租户条件
     */
    @Test
    public void testGetHistoricTask_tenantContextPresent_addTenantCondition() {
        // mock 数据
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        HistoricTaskInstanceQuery query = mock(HistoricTaskInstanceQuery.class);
        when(query.taskId("t1")).thenReturn(query);
        when(query.includeTaskLocalVariables()).thenReturn(query);
        when(query.taskTenantId("123")).thenReturn(query);
        when(query.singleResult()).thenReturn(task);
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(query);
        try (MockedStatic<FlowableUtils> flowableUtilsMocked = mockStatic(FlowableUtils.class)) {
            flowableUtilsMocked.when(FlowableUtils::getTenantId).thenReturn("123");
            // 调用，并断言
            assertSame(task, bpmTaskService.getHistoricTask("t1"));
            verify(query).taskTenantId("123");
        }
    }

    // ========== getTaskListByParentTaskId 对象授权 ==========

    /**
     * 场景：调用人是子任务参与人，允许查询子任务列表
     */
    @Test
    public void testGetTaskListByParentTaskId_childParticipant_success() {
        // mock 数据
        Task childTask = mock(Task.class);
        when(childTask.getAssignee()).thenReturn("1");
        NativeTaskQuery nativeQuery = mockNativeListQuery("10", ListUtil.of(childTask));
        when(taskService.createNativeTaskQuery()).thenReturn(nativeQuery);
        Task parentTask = mock(Task.class);
        when(parentTask.getAssignee()).thenReturn("2");
        TaskQuery parentQuery = mock(TaskQuery.class);
        mockGetTaskQuery(parentQuery, "10", parentTask);
        when(taskService.createTaskQuery()).thenReturn(parentQuery);
        when(managementService.getTableName(TaskEntity.class)).thenReturn("act_ru_task");
        // 调用，并断言
        assertEquals(ListUtil.of(childTask), bpmTaskService.getTaskListByParentTaskId(1L, "10"));
    }

    /**
     * 场景：调用人是父任务参与人，允许查询子任务列表
     */
    @Test
    public void testGetTaskListByParentTaskId_parentParticipant_success() {
        // mock 数据
        Task childTask = mock(Task.class);
        NativeTaskQuery nativeQuery = mockNativeListQuery("10", ListUtil.of(childTask));
        when(taskService.createNativeTaskQuery()).thenReturn(nativeQuery);
        Task parentTask = mock(Task.class);
        when(parentTask.getAssignee()).thenReturn("1");
        TaskQuery parentQuery = mock(TaskQuery.class);
        mockGetTaskQuery(parentQuery, "10", parentTask);
        when(taskService.createTaskQuery()).thenReturn(parentQuery);
        when(managementService.getTableName(TaskEntity.class)).thenReturn("act_ru_task");
        // 调用，并断言
        assertEquals(ListUtil.of(childTask), bpmTaskService.getTaskListByParentTaskId(1L, "10"));
    }

    /**
     * 场景：调用人既不是父任务参与人、也不是子任务参与人，查询失败
     */
    @Test
    public void testGetTaskListByParentTaskId_notParticipant_throw() {
        // mock 数据
        Task childTask = mock(Task.class);
        when(childTask.getAssignee()).thenReturn("2");
        NativeTaskQuery nativeQuery = mockNativeListQuery("10", ListUtil.of(childTask));
        when(taskService.createNativeTaskQuery()).thenReturn(nativeQuery);
        Task parentTask = mock(Task.class);
        when(parentTask.getAssignee()).thenReturn("3");
        TaskQuery parentQuery = mock(TaskQuery.class);
        mockGetTaskQuery(parentQuery, "10", parentTask);
        when(taskService.createTaskQuery()).thenReturn(parentQuery);
        when(managementService.getTableName(TaskEntity.class)).thenReturn("act_ru_task");
        // 调用，并断言
        assertServiceException(() -> bpmTaskService.getTaskListByParentTaskId(1L, "10"),
                TASK_OPERATE_FAIL_NOT_PARTICIPANT);
    }

    // ========== deleteSignTask 对象授权 ==========

    /**
     * 场景：调用人是被减签任务参与人，允许减签
     */
    @Test
    public void testDeleteSignTask_taskParticipant_success() {
        // mock 数据
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("t1");
        when(task.getAssignee()).thenReturn("1");
        when(task.getParentTaskId()).thenReturn("10");
        when(task.getProcessInstanceId()).thenReturn("p1");
        TaskQuery query = mock(TaskQuery.class);
        mockGetTaskQuery(query, "t1", task);
        Task parentTask = mock(Task.class);
        when(parentTask.getScopeType()).thenReturn(BpmTaskSignTypeEnum.BEFORE.getType());
        TaskQuery parentQuery = mock(TaskQuery.class);
        mockGetTaskQuery(parentQuery, "10", parentTask);
        when(taskService.createTaskQuery()).thenReturn(query, parentQuery);
        AdminUserRespDTO user = randomPojo(AdminUserRespDTO.class);
        when(adminUserApi.getUser(1L)).thenReturn(user);
        NativeTaskQuery childQuery = mockNativeListQuery("t1", Collections.emptyList());
        NativeTaskQuery countQuery = mockNativeCountQuery("10", 1L);
        when(taskService.createNativeTaskQuery()).thenReturn(childQuery, countQuery);
        when(managementService.getTableName(TaskEntity.class)).thenReturn("act_ru_task");
        // 调用
        BpmTaskSignDeleteReqVO reqVO = new BpmTaskSignDeleteReqVO();
        reqVO.setId("t1");
        reqVO.setReason("减签原因");
        bpmTaskService.deleteSignTask(1L, reqVO);
        // 断言：任务被删除 + 记录减签评论
        verify(taskService).deleteTasks(anyList());
        verify(commentService).createComment(eq("10"), eq("p1"), eq(BpmCommentTypeEnum.SUB_SIGN), any(), any());
    }

    /**
     * 场景：调用人是父任务参与人（非被减签任务参与人），允许减签
     */
    @Test
    public void testDeleteSignTask_parentParticipant_success() {
        // mock 数据
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("t1");
        when(task.getAssignee()).thenReturn("2");
        when(task.getParentTaskId()).thenReturn("10");
        when(task.getProcessInstanceId()).thenReturn("p1");
        TaskQuery query = mock(TaskQuery.class);
        mockGetTaskQuery(query, "t1", task);
        Task parentTask = mock(Task.class);
        when(parentTask.getAssignee()).thenReturn("1");
        when(parentTask.getScopeType()).thenReturn(BpmTaskSignTypeEnum.BEFORE.getType());
        TaskQuery parentQuery = mock(TaskQuery.class);
        mockGetTaskQuery(parentQuery, "10", parentTask);
        TaskQuery parentQuery2 = mock(TaskQuery.class);
        mockGetTaskQuery(parentQuery2, "10", parentTask);
        when(taskService.createTaskQuery()).thenReturn(query, parentQuery, parentQuery2);
        AdminUserRespDTO cancelUser = randomPojo(AdminUserRespDTO.class);
        AdminUserRespDTO currentUser = randomPojo(AdminUserRespDTO.class);
        when(adminUserApi.getUser(2L)).thenReturn(cancelUser);
        when(adminUserApi.getUser(1L)).thenReturn(currentUser);
        NativeTaskQuery childQuery = mockNativeListQuery("t1", Collections.emptyList());
        NativeTaskQuery countQuery = mockNativeCountQuery("10", 1L);
        when(taskService.createNativeTaskQuery()).thenReturn(childQuery, countQuery);
        when(managementService.getTableName(TaskEntity.class)).thenReturn("act_ru_task");
        // 调用
        BpmTaskSignDeleteReqVO reqVO = new BpmTaskSignDeleteReqVO();
        reqVO.setId("t1");
        reqVO.setReason("减签原因");
        bpmTaskService.deleteSignTask(1L, reqVO);
        // 断言：任务被删除 + 记录减签评论
        verify(taskService).deleteTasks(anyList());
        verify(commentService).createComment(eq("10"), eq("p1"), eq(BpmCommentTypeEnum.SUB_SIGN), any(), any());
    }

    /**
     * 场景：调用人既不是被减签任务参与人、也不是父任务参与人，减签失败
     */
    @Test
    public void testDeleteSignTask_notParticipant_throw() {
        // mock 数据
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("t1");
        when(task.getAssignee()).thenReturn("2");
        when(task.getParentTaskId()).thenReturn("10");
        when(task.getProcessInstanceId()).thenReturn("p1");
        TaskQuery query = mock(TaskQuery.class);
        mockGetTaskQuery(query, "t1", task);
        Task parentTask = mock(Task.class);
        when(parentTask.getAssignee()).thenReturn("3");
        when(parentTask.getScopeType()).thenReturn(BpmTaskSignTypeEnum.BEFORE.getType());
        TaskQuery parentQuery = mock(TaskQuery.class);
        mockGetTaskQuery(parentQuery, "10", parentTask);
        when(taskService.createTaskQuery()).thenReturn(query, parentQuery);
        AdminUserRespDTO cancelUser = randomPojo(AdminUserRespDTO.class);
        when(adminUserApi.getUser(2L)).thenReturn(cancelUser);
        // 调用，并断言
        BpmTaskSignDeleteReqVO reqVO = new BpmTaskSignDeleteReqVO();
        reqVO.setId("t1");
        reqVO.setReason("减签原因");
        assertServiceException(() -> bpmTaskService.deleteSignTask(1L, reqVO), TASK_OPERATE_FAIL_NOT_PARTICIPANT);
    }

    // ========== Helper 方法 ==========

    /**
     * mock getTask 内部的 TaskQuery 查询链
     */
    private void mockGetTaskQuery(TaskQuery query, String taskId, Task task) {
        when(query.taskId(taskId)).thenReturn(query);
        when(query.includeTaskLocalVariables()).thenReturn(query);
        when(query.singleResult()).thenReturn(task);
    }

    /**
     * mock queryChildTasks 内部的 NativeTaskQuery 查询链（list 查询）
     */
    private NativeTaskQuery mockNativeListQuery(String parentTaskId, List<Task> tasks) {
        NativeTaskQuery query = mock(NativeTaskQuery.class);
        when(query.sql(anyString())).thenReturn(query);
        when(query.parameter("parentTaskId", parentTaskId)).thenReturn(query);
        when(query.list()).thenReturn(tasks);
        return query;
    }

    /**
     * mock getTaskCountByParentTaskId 内部的 NativeTaskQuery 查询链（count 查询）
     */
    private NativeTaskQuery mockNativeCountQuery(String parentTaskId, long count) {
        NativeTaskQuery query = mock(NativeTaskQuery.class);
        when(query.sql(anyString())).thenReturn(query);
        when(query.parameter("parentTaskId", parentTaskId)).thenReturn(query);
        when(query.count()).thenReturn(count);
        return query;
    }

}
