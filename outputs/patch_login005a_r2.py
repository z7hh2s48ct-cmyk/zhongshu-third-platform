import io

p = r"E:\zszj-wt-login-005-a\services\zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\service\oauth2\OAuth2TokenServiceImpl.java"
s = io.open(p, encoding='utf-8').read()

# ===== P2-A: self-heal uses resilient revokeWithTombstone =====
old1 = '''            if (count <= 0) {
                oauth2AccessTokenRedisDAO.markRevoked(accessToken, millisUntil(accessTokenDO.getExpiresTime()));
                oauth2AccessTokenRedisDAO.delete(accessToken);
                throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "访问令牌不存在");
            }'''
new1 = '''            if (count <= 0) {
                // 自愈复用 revokeWithTombstone（墓碑/删除各自容错，codex r1 P2：避免墓碑失败中断删除、
                // Redis 异常被误记为 DB 核验异常）；随后仍按 401 拒绝
                revokeWithTombstone(accessToken, accessTokenDO.getExpiresTime());
                throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "访问令牌不存在");
            }'''
assert old1 in s, "old1"
s = s.replace(old1, new1)

# ===== P3: single execution at afterCompletion (all statuses), no double run =====
old2 = '''        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeAll.run();
                }

                @Override
                public void afterCompletion(int status) {
                    safeAll.run(); // 幂等兜底：回滚 / STATUS_UNKNOWN / afterCommit 被前序回调异常跳过
                }
            });
        } else {
            safeAll.run();
        }
    }'''
new2 = '''        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // ZS-LOGIN-005.A codex r1 P3：单次执行于事务结束后（afterCompletion 覆盖全部结束状态）——
            // COMMITTED 即「提交后失效」；ROLLED_BACK / STATUS_UNKNOWN 为幂等兜底（回滚补偿 +
            // 前序同步回调异常跳过 afterCommit 的情形一并覆盖），且避免 afterCommit+afterCompletion 双跑翻倍
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    safeAll.run();
                }
            });
        } else {
            safeAll.run();
        }
    }'''
assert old2 in s, "old2"
s = s.replace(old2, new2)

old3 = '''        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safePublish.run();
                }

                @Override
                public void afterCompletion(int status) {
                    // ZS-LOGIN-005.A codex r0 P2：STATUS_UNKNOWN（提交/回滚异常）按保守清理处理——
                    // 误清缓存只损失可用性（miss 回源重建），残留幽灵发布则是安全缺口
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        try {
                            rollbackCleanup.run();
                        } catch (Exception ex) {
                            log.warn("[publishCache][ZS-LOGIN-005.A {} 回滚/未知状态清理失败]", desc, ex);
                        }
                    }
                }
            });
        } else {
            safePublish.run();
        }
    }'''
new3 = '''        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // ZS-LOGIN-005.A：与失效动作一致，单次执行于事务结束后——COMMITTED 才发布；
            // ROLLED_BACK / STATUS_UNKNOWN（提交/回滚异常）按保守清理处理：误清缓存只损失可用性
            // （miss 回源重建），残留幽灵发布则是安全缺口
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_COMMITTED) {
                        safePublish.run();
                    } else {
                        try {
                            rollbackCleanup.run();
                        } catch (Exception ex) {
                            log.warn("[publishCache][ZS-LOGIN-005.A {} 回滚/未知状态清理失败]", desc, ex);
                        }
                    }
                }
            });
        } else {
            safePublish.run();
        }
    }'''
assert old3 in s, "old3"
s = s.replace(old3, new3)

io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('service r2 ok')

# ===== tests: fault-then-continue + pre-callback-exception + session-cache recheck =====
# A) AuthorityUnitTest: markRevoked fails but delete + generation cleanup still run
p2 = r"E:\zszj-wt-login-005-a\services\zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\service\oauth2\OAuth2TokenServiceImplAuthorityUnitTest.java"
s = io.open(p2, encoding='utf-8').read()
old4 = '''    @Test
    void batchRevoke_redisFailure_dbStillAuthoritativeAndStepsIsolated() {
        Long userId = 470L;
        String orphan = "eee-orphan-refresh";
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()))
                .thenReturn(List.of());
        when(refreshTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()))
                .thenReturn(List.of(makeRefreshToken(orphan, userId)));
        when(accessTokenMapper.selectListByRefreshToken(orphan)).thenReturn(List.of());
        // Redis 全线故障
        doThrow(new RuntimeException("redis down")).when(redisDAO).delete(anyString());
        doThrow(new RuntimeException("redis down")).when(redisDAO).markRevoked(anyString(), anyLong());

        // 撤销不得因 Redis 失败而失败——DB 权威删除照常完成
        assertDoesNotThrow(() -> service.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue()));

        verify(refreshTokenMapper, times(1)).deleteByRefreshToken(orphan);
    }'''
new4 = '''    @Test
    void batchRevoke_redisFailure_dbStillAuthoritativeAndStepsIsolated() {
        Long userId = 470L;
        String orphan = "eee-orphan-refresh";
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()))
                .thenReturn(List.of());
        when(refreshTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()))
                .thenReturn(List.of(makeRefreshToken(orphan, userId)));
        when(accessTokenMapper.selectListByRefreshToken(orphan)).thenReturn(List.of());
        // Redis 全线故障
        doThrow(new RuntimeException("redis down")).when(redisDAO).delete(anyString());
        doThrow(new RuntimeException("redis down")).when(redisDAO).markRevoked(anyString(), anyLong());

        // 撤销不得因 Redis 失败而失败——DB 权威删除照常完成
        assertDoesNotThrow(() -> service.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue()));

        verify(refreshTokenMapper, times(1)).deleteByRefreshToken(orphan);
    }

    @Test
    void batchRevoke_tombstoneFails_deleteAndLaterStepsStillRun() {
        // codex r1 P2：墓碑写入失败后，删缓存与后续步骤（代际键清理）必须继续执行——逐项隔离
        Long userId = 480L;
        String orphan = "fff-orphan-refresh";
        when(accessTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()))
                .thenReturn(List.of());
        when(refreshTokenMapper.selectListByUserIdAndUserType(userId, UserTypeEnum.ADMIN.getValue()))
                .thenReturn(List.of(makeRefreshToken(orphan, userId)));
        when(accessTokenMapper.selectListByRefreshToken(orphan)).thenReturn(List.of());
        doThrow(new RuntimeException("tombstone down")).when(redisDAO).markRevoked(anyString(), anyLong());

        assertDoesNotThrow(() -> service.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue()));

        verify(redisDAO, times(1)).delete(orphan); // 墓碑失败不中断删缓存
        verify(redisDAO, times(1)).deleteSessionGeneration(orphan); // 后续步骤继续
        verify(refreshTokenMapper, times(1)).deleteByRefreshToken(orphan);
    }'''
assert old4 in s, "old4"
s = s.replace(old4, new4)
io.open(p2, 'w', encoding='utf-8', newline='\n').write(s)
print('authority unit test ok')

# B) CacheConsistencyTest: pre-callback-exception fallback + session-cache recheck
p3 = r"E:\zszj-wt-login-005-a\services\zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\service\oauth2\OAuth2TokenServiceImplCacheConsistencyTest.java"
s = io.open(p3, encoding='utf-8').read()
old5 = '''    // ========== 工具方法 =========='''
if old5 not in s:
    old5 = None
# append two tests before final closing brace
idx = s.rstrip().rfind('}')
addition = '''
    // ========== ④ codex r1：前序同步回调异常跳过 afterCommit 后，事务结束兜底仍完成失效 ==========

    /**
     * 前序同步回调在 afterCommit 抛异常会跳过同事务内后续 afterCommit 回调；
     * afterCompletion 兜底必须仍完成缓存失效（修复前若只挂 afterCommit 则失效被跳过）。
     */
    @Test
    public void testRemoveAccessTokenByUser_priorCallbackThrows_fallbackStillInvalidates() {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2AccessTokenDO access = seedSession(clientId, userId);
        assertNotNull(oauth2TokenService.getAccessToken(access.getAccessToken()), "前置：预热缓存条目存在");

        transactionTemplate.executeWithoutResult(status -> {
            // 故意先注册一个 afterCommit 抛异常的同步回调（先于 service 内部注册的回调执行）
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            throw new RuntimeException("前序回调异常");
                        }
                    });
            oauth2TokenService.removeAccessToken(userId, UserTypeEnum.ADMIN.getValue());
        });

        assertNull(oauth2AccessTokenRedisDAO.get(access.getAccessToken()),
                "前序 afterCommit 异常跳过后，事务结束兜底必须仍完成缓存失效");
    }

    // ========== ⑤ codex r1：flushCache 权威核验绕开 MyBatis SESSION 一级缓存旧快照 ==========

    /**
     * 长事务内（同 - SqlSession 复用）：T1 首次权威核验通过后，T2 在另一连接撤销并提交，
     * T1 再次核验必须拒绝——普通 select 会命中一级缓存旧快照放行，flushCache=TRUE count 必须回源。
     */
    @Test
    public void testCheckAccessToken_withinOpenTx_recheckAfterExternalRevoke_rejects() throws Exception {
        String clientId = randomString();
        mockClient(clientId);
        Long userId = randomLongId();
        OAuth2AccessTokenDO access = seedSession(clientId, userId);
        assertNotNull(oauth2TokenService.getAccessToken(access.getAccessToken()), "前置：预热缓存条目存在");

        java.util.concurrent.CountDownLatch t1Checked = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch t2Committed = new java.util.concurrent.CountDownLatch(1);
        AtomicReference<Throwable> t2Error = new AtomicReference<>();

        Thread t2 = new Thread(() -> {
            try {
                assertTrue(t1Checked.await(10, java.util.concurrent.TimeUnit.SECONDS), "等待 T1 首次核验超时");
                transactionTemplate.executeWithoutResult(status ->
                        oauth2TokenService.removeAccessToken(access.getAccessToken()));
            } catch (Throwable ex) {
                t2Error.set(ex);
            } finally {
                t2Committed.countDown();
            }
        });
        t2.start();

        transactionTemplate.executeWithoutResult(status -> {
            try {
                // T1 首次核验：行存在，通过
                oauth2TokenService.checkAccessToken(access.getAccessToken());
            } catch (Exception ex) {
                throw new RuntimeException("T1 首次核验不应失败", ex);
            }
            t1Checked.countDown();
            try {
                assertTrue(t2Committed.await(10, java.util.concurrent.TimeUnit.SECONDS), "等待 T2 撤销提交超时");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            }
            // T1 同事务内再次核验：T2 已在另一连接撤销并提交——flushCache=TRUE 必须回源拒绝
            assertServiceException(() -> oauth2TokenService.checkAccessToken(access.getAccessToken()),
                    new ErrorCode(401, "访问令牌不存在"));
        });

        assertNull(t2Error.get(), "T2 撤销不得失败: " + t2Error.get());
    }
}
'''
s = s[:idx] + addition
io.open(p3, 'w', encoding='utf-8', newline='\n').write(s)
print('consistency tests ok')
