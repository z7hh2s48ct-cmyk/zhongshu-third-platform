package cn.zszj.module.infra.framework.idempotent;

import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentRecord;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentStatus;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link JdbcPersistentIdempotentStore} 单元测试（ZS-SEC-011.B，H2）。
 *
 * 覆盖 SPI 契约的 DB 层语义：唯一约束并发兜底（ON CONFLICT DO NOTHING 不毒化事务、败者可读回）、
 * MANDATORY 事务参与（无事务 fail-closed；业务回滚无残留记录）、状态机（RUNNING→SUCCESS/FAILED、
 * 删记录可重插）。真实 PG 上的并发抢占与迁移重放见 scripts/db/run-sec011b-verify.mjs。
 */
@Import(JdbcPersistentIdempotentStore.class)
public class JdbcPersistentIdempotentStoreTest extends BaseDbUnitTest {

    private static final String KEY = "test-key-0001";

    @Resource
    private JdbcPersistentIdempotentStore store;

    @Resource
    private DataSource dataSource;

    @Resource
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx() {
        return new TransactionTemplate(transactionManager);
    }

    private PersistentIdempotentRecord runningRecord(String key, String digest) {
        PersistentIdempotentRecord record = new PersistentIdempotentRecord();
        record.setIdempotentKey(key);
        record.setTenantId(1L);
        record.setSubjectType("2");
        record.setSubjectId("100");
        record.setActionScope("OrderService.createOrder(..)");
        record.setRequestDigest(digest);
        record.setStatus(PersistentIdempotentStatus.RUNNING);
        return record;
    }

    @Test
    void tryInsertRunning_insertsAndReadsBackAllColumns() {
        Boolean acquired = tx().execute(status -> store.tryInsertRunning(runningRecord(KEY, "digest-1")));
        assertEquals(Boolean.TRUE, acquired, "首次插入应获得执行权");

        Optional<PersistentIdempotentRecord> found = store.findByIdempotentKey(KEY);
        assertTrue(found.isPresent(), "应能按键读回记录");
        PersistentIdempotentRecord record = found.get();
        assertEquals(KEY, record.getIdempotentKey());
        assertEquals(1L, record.getTenantId());
        assertEquals("2", record.getSubjectType());
        assertEquals("100", record.getSubjectId());
        assertEquals("OrderService.createOrder(..)", record.getActionScope());
        assertEquals("digest-1", record.getRequestDigest());
        assertEquals(PersistentIdempotentStatus.RUNNING, record.getStatus());
        assertNull(record.getResultSnapshot(), "RUNNING 时无快照");
        assertNotNull(record.getCreateTime(), "create_time 由 DB 默认值填充");
        assertNull(record.getCompleteTime(), "RUNNING 时无完成时间");
    }

    @Test
    void tryInsertRunning_duplicate_returnsFalseAndKeepsOriginalRecord() {
        tx().execute(status -> store.tryInsertRunning(runningRecord(KEY, "digest-original")));
        // 并发败者：同键不同摘要再插 → false（唯一约束兜底），且不得毒化事务（本模板事务正常提交）
        Boolean acquired = tx().execute(status -> store.tryInsertRunning(runningRecord(KEY, "digest-other")));
        assertEquals(Boolean.FALSE, acquired, "同键二次插入应被唯一约束兜底拒绝");

        Optional<PersistentIdempotentRecord> found = store.findByIdempotentKey(KEY);
        assertTrue(found.isPresent());
        assertEquals("digest-original", found.get().getRequestDigest(), "败者不得覆盖胜者记录（保留原始摘要）");
    }

    @Test
    void markSuccess_transitionsWithSnapshot() {
        tx().execute(status -> store.tryInsertRunning(runningRecord(KEY, "digest-1")));
        tx().execute(status -> {
            store.markSuccess(KEY, "\"result-json\"");
            return null;
        });

        PersistentIdempotentRecord record = store.findByIdempotentKey(KEY).orElseThrow();
        assertEquals(PersistentIdempotentStatus.SUCCESS, record.getStatus());
        assertEquals("\"result-json\"", record.getResultSnapshot());
        assertNotNull(record.getCompleteTime(), "完成时间应被写入");
    }

    @Test
    void markSuccess_onNotRunning_failsClosed() {
        // 键不存在 → 状态守卫（WHERE status='RUNNING'）未命中 → fail-closed 抛出（事务回滚兜底）
        assertThrows(Exception.class, () -> tx().execute(status -> {
            store.markSuccess(KEY, "\"x\"");
            return null;
        }));
    }

    @Test
    void markFailed_keepsRowAndBlocksReinsert() {
        tx().execute(status -> store.tryInsertRunning(runningRecord(KEY, "digest-1")));
        tx().execute(status -> {
            store.markFailed(KEY);
            return null;
        });

        PersistentIdempotentRecord record = store.findByIdempotentKey(KEY).orElseThrow();
        assertEquals(PersistentIdempotentStatus.FAILED, record.getStatus());
        // FAILED 保留记录：重插仍被唯一约束兜底拒绝（重放由切面按 FAILED 拒绝，不重复执行业务）
        Boolean reInsert = tx().execute(status -> store.tryInsertRunning(runningRecord(KEY, "digest-2")));
        assertEquals(Boolean.FALSE, reInsert);
    }

    @Test
    void deleteRunning_allowsReinsert() {
        tx().execute(status -> store.tryInsertRunning(runningRecord(KEY, "digest-1")));
        Boolean deleted = tx().execute(status -> store.deleteRunning(KEY));
        assertEquals(Boolean.TRUE, deleted, "RUNNING 记录应可删除");
        assertTrue(store.findByIdempotentKey(KEY).isEmpty(), "删除后记录不可读");

        Boolean reInserted = tx().execute(status -> store.tryInsertRunning(runningRecord(KEY, "digest-2")));
        assertEquals(Boolean.TRUE, reInserted, "失败删记录后客户端可重试（同键重新获得执行权）");
    }

    @Test
    void deleteRunning_onMissing_returnsFalse() {
        Boolean deleted = tx().execute(status -> store.deleteRunning(KEY));
        assertEquals(Boolean.FALSE, deleted);
    }

    @Test
    void longSnapshotAndDigest_roundtripWithoutTruncation() {
        // >2048 字符快照 + 中文：SQL 层不截断（P2-1 的存储侧对称——摘要/快照必须全量落库）
        String longSnapshot = "{\"note\":\"" + "长".repeat(1500) + "-tail-" + "x".repeat(1000) + "\"}";
        tx().execute(status -> store.tryInsertRunning(runningRecord(KEY, "digest-long")));
        tx().execute(status -> {
            store.markSuccess(KEY, longSnapshot);
            return null;
        });
        assertEquals(longSnapshot, store.findByIdempotentKey(KEY).orElseThrow().getResultSnapshot(),
                "长快照必须全量往返，不得截断");
    }

    @Test
    void tryInsertRunning_withoutTransaction_failsClosed() {
        // SPI 契约①：MANDATORY——无调用方事务直接拒绝，不静默自提交
        IllegalTransactionStateException ex = assertThrows(IllegalTransactionStateException.class,
                () -> store.tryInsertRunning(runningRecord(KEY, "digest-1")));
        assertNotNull(ex.getMessage());
        assertTrue(store.findByIdempotentKey(KEY).isEmpty(), "无事务写入不得落库");
    }

    @Test
    void transactionRollback_leavesNoResidualRecord() {
        // 业务回滚 → RUNNING 记录随调用方事务一并回滚（同生共死），重放可安全重执行
        assertThrows(IllegalStateException.class, () -> tx().execute(status -> {
            store.tryInsertRunning(runningRecord(KEY, "digest-1"));
            throw new IllegalStateException("business boom");
        }));
        assertTrue(store.findByIdempotentKey(KEY).isEmpty(), "事务回滚后不得残留记录");
    }

    @Test
    void differentKeys_areIndependentRecords() {
        tx().execute(status -> store.tryInsertRunning(runningRecord("key-A", "digest-A")));
        tx().execute(status -> store.tryInsertRunning(runningRecord("key-B", "digest-B")));
        assertNotEquals(
                store.findByIdempotentKey("key-A").orElseThrow().getRequestDigest(),
                store.findByIdempotentKey("key-B").orElseThrow().getRequestDigest());
        assertFalse(store.findByIdempotentKey("key-A").orElseThrow().getIdempotentKey()
                .equals(store.findByIdempotentKey("key-B").orElseThrow().getIdempotentKey()));
    }

}
