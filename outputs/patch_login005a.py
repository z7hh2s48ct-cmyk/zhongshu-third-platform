import io

p = r"E:\zszj-wt-login-005-a\services\zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\service\oauth2\OAuth2TokenServiceImpl.java"
s = io.open(p, encoding='utf-8').read()

# ===== 3) doRemoveAccessTokenByUser: defer invalidations =====
old3 = '''        // 2. 按稳定顺序逐个锁定并撤销
        for (String refreshToken : sortedRefreshTokens) {
            // ZS-LOGIN-002：行锁 + 获锁后重读
            oauth2RefreshTokenMapper.selectByRefreshTokenForUpdate(refreshToken);
            // 找到该 refresh-token 对应的任一 access-token 作为 fallback；孤立刷新凭据没有 fallback（为 null）
            OAuth2AccessTokenDO fallback = null;
            for (OAuth2AccessTokenDO at : accessTokens) {
                if (refreshToken.equals(at.getRefreshToken())) {
                    fallback = at;
                    break;
                }
            }
            // 获锁后重读，覆盖并发刷新在获锁前刚插入的新代际访问令牌（防会话复活）
            for (OAuth2AccessTokenDO aliveToken : listAliveAccessTokens(refreshToken, fallback)) {
                oauth2AccessTokenMapper.deleteById(aliveToken.getId());
                revokeWithTombstone(aliveToken.getAccessToken(), aliveToken.getExpiresTime());
            }
            // ZS-LOGIN-003 codex r1 P1：删除前先读刷新令牌剩余有效期（删除后查不到），落撤销墓碑，
            // 堵住并发鉴权/门控路径从旧 DB 快照回填复活
            OAuth2RefreshTokenDO revokeTarget = oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken);
            // 删除刷新令牌
            oauth2RefreshTokenMapper.deleteByRefreshToken(refreshToken);
            // ZS-LOGIN-003：清除「刷新令牌被当作访问令牌」时缓存下来的转换凭据（ZS-LOGIN-001 兼容路径写入，
            // key 即 refreshToken 串，TTL 继承刷新令牌）；对孤立刷新凭据而言这是唯一的清理时机
            revokeWithTombstone(refreshToken, revokeTarget != null ? revokeTarget.getExpiresTime() : null);
            // ZS-LOGIN-002：会话终结，清理代际键
            deleteSessionGenerationQuietly(refreshToken);
        }'''
new3 = '''        // 2. 按稳定顺序逐个锁定并撤销（缓存失效统一注册到事务提交后执行——ZS-LOGIN-005.A）
        List<Runnable> cacheInvalidations = new ArrayList<>();
        for (String refreshToken : sortedRefreshTokens) {
            // ZS-LOGIN-002：行锁 + 获锁后重读
            oauth2RefreshTokenMapper.selectByRefreshTokenForUpdate(refreshToken);
            // 找到该 refresh-token 对应的任一 access-token 作为 fallback；孤立刷新凭据没有 fallback（为 null）
            OAuth2AccessTokenDO fallback = null;
            for (OAuth2AccessTokenDO at : accessTokens) {
                if (refreshToken.equals(at.getRefreshToken())) {
                    fallback = at;
                    break;
                }
            }
            // 获锁后重读，覆盖并发刷新在获锁前刚插入的新代际访问令牌（防会话复活）
            for (OAuth2AccessTokenDO aliveToken : listAliveAccessTokens(refreshToken, fallback)) {
                oauth2AccessTokenMapper.deleteById(aliveToken.getId());
                cacheInvalidations.add(() -> revokeWithTombstone(aliveToken.getAccessToken(), aliveToken.getExpiresTime()));
            }
            // ZS-LOGIN-003 codex r1 P1：删除前先读刷新令牌剩余有效期（删除后查不到），落撤销墓碑，
            // 堵住并发鉴权/门控路径从旧 DB 快照回填复活
            OAuth2RefreshTokenDO revokeTarget = oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken);
            // 删除刷新令牌
            oauth2RefreshTokenMapper.deleteByRefreshToken(refreshToken);
            // ZS-LOGIN-003：清除「刷新令牌被当作访问令牌」时缓存下来的转换凭据（ZS-LOGIN-001 兼容路径写入，
            // key 即 refreshToken 串，TTL 继承刷新令牌）；对孤立刷新凭据而言这是唯一的清理时机
            cacheInvalidations.add(() -> revokeWithTombstone(refreshToken,
                    revokeTarget != null ? revokeTarget.getExpiresTime() : null));
            // ZS-LOGIN-002：会话终结，清理代际键
            cacheInvalidations.add(() -> deleteSessionGenerationQuietly(refreshToken));
        }
        // ZS-LOGIN-005.A：缓存失效注册到事务提交后执行（回滚亦执行失效补偿），失败告警不阻断
        invalidateCacheAfterCommit("用户级撤销(removeAccessTokenByUser)", () -> cacheInvalidations.forEach(Runnable::run));'''
assert old3 in s, "old3 not found"
s = s.replace(old3, new3)

# ===== 4) refreshAccessToken: evictions deferred =====
old4 = '''        List<OAuth2AccessTokenDO> accessTokenDOs = oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken);
        if (CollUtil.isNotEmpty(accessTokenDOs)) {
            oauth2AccessTokenMapper.deleteByIds(convertSet(accessTokenDOs, OAuth2AccessTokenDO::getId));
            for (OAuth2AccessTokenDO evicted : accessTokenDOs) {
                revokeWithTombstone(evicted.getAccessToken(), evicted.getExpiresTime());
            }
        }'''
new4 = '''        List<OAuth2AccessTokenDO> accessTokenDOs = oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken);
        if (CollUtil.isNotEmpty(accessTokenDOs)) {
            oauth2AccessTokenMapper.deleteByIds(convertSet(accessTokenDOs, OAuth2AccessTokenDO::getId));
            // ZS-LOGIN-005.A：旧代际缓存失效注册到事务提交后执行（回滚亦补偿）
            List<Runnable> evictions = new ArrayList<>();
            for (OAuth2AccessTokenDO evicted : accessTokenDOs) {
                evictions.add(() -> revokeWithTombstone(evicted.getAccessToken(), evicted.getExpiresTime()));
            }
            invalidateCacheAfterCommit("刷新淘汰旧代际(refreshAccessToken)", () -> evictions.forEach(Runnable::run));
        }'''
assert old4 in s, "old4 not found"
s = s.replace(old4, new4)

# ===== 5) createOAuth2AccessToken: publish after commit =====
old5 = '''        oauth2AccessTokenMapper.insert(accessTokenDO);
        // 记录到 Redis 中
        oauth2AccessTokenRedisDAO.set(accessTokenDO);
        return accessTokenDO;
    }'''
new5 = '''        oauth2AccessTokenMapper.insert(accessTokenDO);
        // 记录到 Redis 中
        // ZS-LOGIN-005.A：缓存发布注册到事务提交后执行——事务回滚时不得残留「DB 无行、Redis 有值」的幽灵新令牌
        publishCacheAfterCommit("新访问令牌(createOAuth2AccessToken)",
                () -> oauth2AccessTokenRedisDAO.set(accessTokenDO),
                () -> oauth2AccessTokenRedisDAO.delete(accessTokenDO.getAccessToken()));
        return accessTokenDO;
    }'''
assert old5 in s, "old5 not found"
s = s.replace(old5, new5)

# ===== 6) helpers after revokeWithTombstone =====
old6 = '''    private void revokeWithTombstone(String token, LocalDateTime expiresTime) {
        oauth2AccessTokenRedisDAO.markRevoked(token, millisUntil(expiresTime));
        oauth2AccessTokenRedisDAO.delete(token);
    }'''
new6 = '''    private void revokeWithTombstone(String token, LocalDateTime expiresTime) {
        oauth2AccessTokenRedisDAO.markRevoked(token, millisUntil(expiresTime));
        oauth2AccessTokenRedisDAO.delete(token);
    }

    /**
     * ZS-LOGIN-005.A：把「缓存失效类」Redis 动作（墓碑 + 删缓存 + 代际键）注册到事务<b>提交后</b>执行——
     * 撤销的权威状态在 DB，缓存失效不必也不应在事务内抢跑（事务回滚时 DB 凭据仍有效，缓存不应被提前清理后
     * 无人补偿）。事务回滚时同样执行失效补偿：缓存 miss 可回源重建，宁可可用性抖动也不留撤销态不一致；
     * 无事务上下文（测试 / 自调用）时立即执行。
     *
     * <p>Redis 动作失败<b>不阻断</b> DB 提交：输出结构化 WARN（可重放修复证据），修复路径 =
     * {@link #checkAccessToken} 的 DB 权威校验自愈 evict；可靠重放补偿归 ZS-LOGIN-005.B（B05 JOB-002）。
     */
    private void invalidateCacheAfterCommit(String desc, Runnable invalidation) {
        Runnable safeInvalidation = () -> {
            try {
                invalidation.run();
            } catch (Exception ex) {
                log.warn("[invalidateCache][ZS-LOGIN-005.A {} 缓存失效失败——凭据权威以 DB 为准"
                        + "（checkAccessToken DB 权威校验自愈 evict 即修复路径，可靠重放归 ZS-LOGIN-005.B）]", desc, ex);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeInvalidation.run();
                }

                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        safeInvalidation.run(); // 回滚补偿：清缓存，miss 回源重建
                    }
                }
            });
        } else {
            safeInvalidation.run();
        }
    }

    /**
     * ZS-LOGIN-005.A：把「缓存发布类」Redis 动作（新令牌写缓存）注册到事务<b>提交后</b>执行——
     * 事务回滚时执行回滚清理，杜绝「缓存成功而事务回滚」留下的幽灵新令牌（DB 无行、Redis 有值）；
     * 无事务上下文（测试 / 自调用）时立即发布。
     */
    private void publishCacheAfterCommit(String desc, Runnable publish, Runnable rollbackCleanup) {
        Runnable safePublish = () -> {
            try {
                publish.run();
            } catch (Exception ex) {
                log.warn("[publishCache][ZS-LOGIN-005.A {} 缓存发布失败——新令牌可经缓存 miss 回源重建]", desc, ex);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safePublish.run();
                }

                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        try {
                            rollbackCleanup.run();
                        } catch (Exception ex) {
                            log.warn("[publishCache][ZS-LOGIN-005.A {} 回滚清理失败]", desc, ex);
                        }
                    }
                }
            });
        } else {
            safePublish.run();
        }
    }'''
assert old6 in s, "old6 not found"
s = s.replace(old6, new6)

io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('impl ok')
