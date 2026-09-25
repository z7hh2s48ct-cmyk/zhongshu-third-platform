package cn.zszj.module.bpm.service.task;

import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zszj.module.bpm.dal.mysql.task.BpmProcessInstanceCopyMapper;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.PROCESS_INSTANCE_QUERY_FAIL_NOT_VISIBLE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * {@link BpmInstanceVisibilityChecker} 的单元测试
 *
 * 覆盖：流程发起人、历史任务审批人、历史任务拥有者、流程抄送人、非参与人、内部调用六种场景
 *
 * @author zszj
 */
public class BpmInstanceVisibilityCheckerTest extends BaseMockitoUnitTest {

    private static final Long LOGIN_USER_ID = 10L;
    private static final String PROCESS_INSTANCE_ID = "p1";

    @InjectMocks
    private BpmInstanceVisibilityChecker visibilityChecker;

    @Mock
    private HistoryService historyService;
    @Mock
    private BpmProcessInstanceCopyMapper processInstanceCopyMapper;

    /**
     * 场景：登录用户是流程发起人，可见
     */
    @Test
    public void testCheckProcessInstanceVisible_startUser_success() {
        // mock 数据
        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstance.getStartUserId()).thenReturn("10");
        // 调用，并断言
        assertTrue(visibilityChecker.isProcessInstanceVisible(LOGIN_USER_ID, processInstance));
        visibilityChecker.checkProcessInstanceVisible(LOGIN_USER_ID, processInstance);
        verifyNoInteractions(historyService, processInstanceCopyMapper);
    }

    /**
     * 场景：登录用户是历史任务审批人（assignee），可见
     */
    @Test
    public void testCheckProcessInstanceVisible_historicAssignee_success() {
        // mock 数据
        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstance.getStartUserId()).thenReturn("99");
        when(processInstance.getId()).thenReturn(PROCESS_INSTANCE_ID);
        HistoricTaskInstanceQuery assigneeQuery = mock(HistoricTaskInstanceQuery.class);
        when(assigneeQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(assigneeQuery);
        when(assigneeQuery.taskAssignee("10")).thenReturn(assigneeQuery);
        when(assigneeQuery.count()).thenReturn(1L);
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(assigneeQuery);
        // 调用，并断言
        assertTrue(visibilityChecker.isProcessInstanceVisible(LOGIN_USER_ID, processInstance));
    }

    /**
     * 场景：登录用户是历史任务拥有者（owner），可见
     */
    @Test
    public void testCheckProcessInstanceVisible_historicOwner_success() {
        // mock 数据
        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstance.getStartUserId()).thenReturn("99");
        when(processInstance.getId()).thenReturn(PROCESS_INSTANCE_ID);
        HistoricTaskInstanceQuery assigneeQuery = mock(HistoricTaskInstanceQuery.class);
        when(assigneeQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(assigneeQuery);
        when(assigneeQuery.taskAssignee("10")).thenReturn(assigneeQuery);
        when(assigneeQuery.count()).thenReturn(0L);
        HistoricTaskInstanceQuery ownerQuery = mock(HistoricTaskInstanceQuery.class);
        when(ownerQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(ownerQuery);
        when(ownerQuery.taskOwner("10")).thenReturn(ownerQuery);
        when(ownerQuery.count()).thenReturn(1L);
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(assigneeQuery, ownerQuery);
        // 调用，并断言
        assertTrue(visibilityChecker.isProcessInstanceVisible(LOGIN_USER_ID, processInstance));
    }

    /**
     * 场景：登录用户是流程抄送人，可见
     */
    @Test
    public void testCheckProcessInstanceVisible_copyUser_success() {
        // mock 数据
        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstance.getStartUserId()).thenReturn("99");
        when(processInstance.getId()).thenReturn(PROCESS_INSTANCE_ID);
        HistoricTaskInstanceQuery assigneeQuery = mock(HistoricTaskInstanceQuery.class);
        when(assigneeQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(assigneeQuery);
        when(assigneeQuery.taskAssignee("10")).thenReturn(assigneeQuery);
        when(assigneeQuery.count()).thenReturn(0L);
        HistoricTaskInstanceQuery ownerQuery = mock(HistoricTaskInstanceQuery.class);
        when(ownerQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(ownerQuery);
        when(ownerQuery.taskOwner("10")).thenReturn(ownerQuery);
        when(ownerQuery.count()).thenReturn(0L);
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(assigneeQuery, ownerQuery);
        when(processInstanceCopyMapper.selectCountByUserIdAndProcessInstanceId(LOGIN_USER_ID, PROCESS_INSTANCE_ID))
                .thenReturn(1L);
        // 调用，并断言
        assertTrue(visibilityChecker.isProcessInstanceVisible(LOGIN_USER_ID, processInstance));
    }

    /**
     * 场景：登录用户与流程实例无关联，校验失败
     */
    @Test
    public void testCheckProcessInstanceVisible_notParticipant_throw() {
        // mock 数据
        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstance.getStartUserId()).thenReturn("99");
        when(processInstance.getId()).thenReturn(PROCESS_INSTANCE_ID);
        HistoricTaskInstanceQuery assigneeQuery = mock(HistoricTaskInstanceQuery.class);
        when(assigneeQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(assigneeQuery);
        when(assigneeQuery.taskAssignee("10")).thenReturn(assigneeQuery);
        when(assigneeQuery.count()).thenReturn(0L);
        HistoricTaskInstanceQuery ownerQuery = mock(HistoricTaskInstanceQuery.class);
        when(ownerQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(ownerQuery);
        when(ownerQuery.taskOwner("10")).thenReturn(ownerQuery);
        when(ownerQuery.count()).thenReturn(0L);
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(assigneeQuery, ownerQuery);
        when(processInstanceCopyMapper.selectCountByUserIdAndProcessInstanceId(LOGIN_USER_ID, PROCESS_INSTANCE_ID))
                .thenReturn(0L);
        // 调用，并断言
        assertServiceException(() -> visibilityChecker.checkProcessInstanceVisible(LOGIN_USER_ID, processInstance),
                PROCESS_INSTANCE_QUERY_FAIL_NOT_VISIBLE);
    }

    /**
     * 场景：内部调用（loginUserId 为空），跳过校验
     */
    @Test
    public void testCheckProcessInstanceVisible_nullLoginUser_skips() {
        // mock 数据
        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        // 调用，并断言
        assertTrue(visibilityChecker.isProcessInstanceVisible(null, processInstance));
        visibilityChecker.checkProcessInstanceVisible(null, processInstance);
        verifyNoInteractions(historyService, processInstanceCopyMapper);
    }

}
