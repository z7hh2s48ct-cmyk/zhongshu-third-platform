package cn.zszj.module.system.service.notify.todo;

import cn.zszj.module.system.dal.dataobject.notify.NotifyTodoDO;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipient;

import java.util.List;

/**
 * 业务待办 Service（ZS-MSG-002）——独立于站内信「已读」的业务待办生命周期。
 *
 * <p>核心保证（docs/05 §11 ZS-MSG-002 验收）：
 * <ol>
 *   <li><b>已读独立</b>：本服务不触碰 {@code NotifyMessageDO} 的 readStatus/readTime；阅读消息不完成业务待办，
 *       完成待办不标记消息已读；</li>
 *   <li><b>可靠事件更新</b>：{@link #applyTransition} 在业务事务内经 ZS-JOB-003 {@code ConsumerInboxPort}
 *       幂等消费（重复事件不重复副作用）+ 版本乱序护栏（旧版本不覆盖新状态）+ 应用层终态守卫（不复活已失效待办）；</li>
 *   <li><b>不复制审批引擎</b>：待办是 BPM Flowable 任务权威状态的投影，本服务不实现审批流转逻辑。</li>
 * </ol>
 */
public interface NotifyTodoService {

    /**
     * 幂等注册业务待办（初始 PENDING）。同 (技术租户, sourceType, todoKey) 重复注册返回既有，不重复建。
     *
     * @return 待办 ID
     */
    Long registerTodo(NotifyTodoRegisterCmd cmd);

    /**
     * 事件驱动更新待办生命周期——必须在业务事务内调用（ZS-JOB-003 Inbox MANDATORY）：
     * 重复事件返回首次结果不改状态；乱序旧版本被拒（STALE_VERSION）；终态待办不被后到事件复活（TERMINAL_GUARDED）；
     * 待办不存在返回未生效（NOT_FOUND，Inbox 记录标记 FAILED 可重试）。
     *
     * @return 流转结果（{@link NotifyTodoTransitionResult#isChanged()} 为 false 表示未生效）
     */
    NotifyTodoTransitionResult applyTransition(NotifyTodoTransitionCmd cmd);

    /** 按 ID 获得待办。 */
    NotifyTodoDO getTodo(Long id);

    /** 按收件人 + 状态列待办（status 为空则不限状态），id 升序有界。 */
    List<NotifyTodoDO> listByRecipient(NotifyRecipient recipient, NotifyTodoStatus status, int limit);

    /** 统计收件人未完成（非终态）待办数。 */
    long getUnfinishedCount(NotifyRecipient recipient);

}
