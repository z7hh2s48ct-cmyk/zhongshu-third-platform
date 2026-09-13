import io

p = r"E:\zszj-wt-login-005-a\services\zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\service\oauth2\OAuth2TokenServiceImpl.java"
s = io.open(p, encoding='utf-8').read()

# ===== P1: authority check uses flushCache count queries (bypass MyBatis SESSION local cache) =====
old_check = '''        // ZS-LOGIN-005.A：DB 权威校验——Redis 降级为纯加速，撤销的权威状态在 DB（行逻辑删除）。
        // 缓存可能残留已撤销凭据（撤销路径缓存失效失败 / 回填竞态），命中后必须回源核验：
        // 普通令牌查 access 表；gate 合成令牌（LOGIN-001 兼容路径）查 refresh 表。
        // 查无即已撤销 → 自愈 evict 缓存并按 401 拒绝（幽灵凭据根本闭环，同时是 Redis 失效失败的自愈修复路径）；
        // DB 查询异常失败关闭（安全默认：宁可拒绝也不放行幽灵）并告警。
        try {
            boolean exists = isSyntheticAccessToken(accessTokenDO)
                    ? oauth2RefreshTokenMapper.selectByRefreshToken(accessToken) != null
                    : oauth2AccessTokenMapper.selectByAccessToken(accessToken) != null;
            if (!exists) {
                oauth2AccessTokenRedisDAO.delete(accessToken);
                throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "访问令牌不存在");
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("[checkAccessToken][ZS-LOGIN-005.A DB 权威校验异常，失败关闭拒绝鉴权 token({})]", maskToken(accessToken), ex);
            throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "访问令牌不存在");
        }
        return accessTokenDO;'''
new_check = '''        // ZS-LOGIN-005.A：DB 权威校验——Redis 降级为纯加速，撤销的权威状态在 DB（行逻辑删除）。
        // 缓存可能残留已撤销凭据（撤销路径缓存失效失败 / 回填竞态），命中后必须回源核验：
        // 普通令牌查 access 表；gate 合成令牌（LOGIN-001 兼容路径）查 refresh 表。
        // 核验使用 flushCache=TRUE 的专用 count 语句（codex r0 P1）：MyBatis SESSION 一级缓存
        // 会在长事务内复用 SqlSession 返回撤销前的旧快照，普通 select 不可作权威依据。
        // 查无即已撤销 → 自愈（落墓碑防即时回填复活 + evict 缓存）并按 401 拒绝（幽灵凭据根本闭环，
        // 同时是 Redis 失效失败的自愈修复路径）；DB 查询异常失败关闭（安全默认：宁可拒绝也不放行幽灵）并告警。
        try {
            int count = isSyntheticAccessToken(accessTokenDO)
                    ? oauth2RefreshTokenMapper.selectAuthorityCountByRefreshToken(accessToken)
                    : oauth2AccessTokenMapper.selectAuthorityCountByAccessToken(accessToken);
            if (count <= 0) {
                oauth2AccessTokenRedisDAO.markRevoked(accessToken, millisUntil(accessTokenDO.getExpiresTime()));
                oauth2AccessTokenRedisDAO.delete(accessToken);
                throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "访问令牌不存在");
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("[checkAccessToken][ZS-LOGIN-005.A DB 权威校验异常，失败关闭拒绝鉴权 token({})]", maskToken(accessToken), ex);
            throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "访问令牌不存在");
        }
        return accessTokenDO;'''
assert old_check in s, "old_check"
s = s.replace(old_check, new_check)

# ===== P2-3: revokeWithTombstone per-op resilience =====
old_tomb = '''    private void revokeWithTombstone(String token, LocalDateTime expiresTime) {
        oauth2AccessTokenRedisDAO.markRevoked(token, millisUntil(expiresTime));
        oauth2AccessTokenRedisDAO.delete(token);
    }'''
new_tomb = '''    private void revokeWithTombstone(String token, LocalDateTime expiresTime) {
        // ZS-LOGIN-005.A codex r0 P2：墓碑与删缓存逐项隔离——墓碑写失败仍须尝试删缓存，反之亦然
        long ttlMillis = millisUntil(expiresTime);
        try {
            oauth2AccessTokenRedisDAO.markRevoked(token, ttlMillis);
        } catch (Exception ex) {
            log.warn("[revokeWithTombstone][ZS-LOGIN-005.A 墓碑写入失败 token({}) 剩余ttl(ms)({})]"
                    + "——继续尝试删缓存，凭据权威以 DB 为准]", maskToken(token), ttlMillis, ex);
        }
        try {
            oauth2AccessTokenRedisDAO.delete(token);
        } catch (Exception ex) {
            log.warn("[revokeWithTombstone][ZS-LOGIN-005.A 缓存删除失败 token({})]"
                    + "——checkAccessToken DB 权威校验自愈 evict 即修复路径]", maskToken(token), ex);
        }
    }'''
assert old_tomb in s, "old_tomb"
s = s.replace(old_tomb, new_tomb)

# ===== P2-2 + P2-1: per-step isolation + all-status idempotent afterCompletion =====
old_inv = '''    private void invalidateCacheAfterCommit(String desc, Runnable invalidation) {
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
    }'''
new_inv = '''    private void invalidateCacheAfterCommit(String desc, List<Runnable> invalidationSteps) {
        // ZS-LOGIN-005.A codex r0 P2：逐项隔离——任一步失败不中断剩余队列，每步分别记录修复证据；
        // afterCompletion 对【所有】结束状态幂等兜底（提交/回滚/UNKNOWN）：失效动作幂等（墓碑+删除），
        // 覆盖 STATUS_UNKNOWN（提交/回滚异常）与前序同步回调抛异常导致 afterCommit 被跳过的情形
        Runnable safeAll = () -> {
            for (int i = 0; i < invalidationSteps.size(); i++) {
                try {
                    invalidationSteps.get(i).run();
                } catch (Exception ex) {
                    log.warn("[invalidateCache][ZS-LOGIN-005.A {} 第 {}/{} 步缓存失效失败——凭据权威以 DB 为准"
                            + "（checkAccessToken DB 权威校验自愈 evict 即修复路径，可靠重放归 ZS-LOGIN-005.B）]",
                            desc, i + 1, invalidationSteps.size(), ex);
                }
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
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
assert old_inv in s, "old_inv"
s = s.replace(old_inv, new_inv)

# publish helper: UNKNOWN -> conservative cleanup
old_pub = '''                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        try {
                            rollbackCleanup.run();
                        } catch (Exception ex) {
                            log.warn("[publishCache][ZS-LOGIN-005.A {} 回滚清理失败]", desc, ex);
                        }
                    }
                }'''
new_pub = '''                @Override
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
                }'''
assert old_pub in s, "old_pub"
s = s.replace(old_pub, new_pub)

# call sites: pass lists directly (steps already individually resilient)
s = s.replace('        invalidateCacheAfterCommit("退出撤销(removeAccessToken)", () -> cacheInvalidations.forEach(Runnable::run));',
              '        invalidateCacheAfterCommit("退出撤销(removeAccessToken)", cacheInvalidations);')
s = s.replace('        invalidateCacheAfterCommit("用户级撤销(removeAccessTokenByUser)", () -> cacheInvalidations.forEach(Runnable::run));',
              '        invalidateCacheAfterCommit("用户级撤销(removeAccessTokenByUser)", cacheInvalidations);')
s = s.replace('            invalidateCacheAfterCommit("刷新淘汰旧代际(refreshAccessToken)", () -> evictions.forEach(Runnable::run));',
              '            invalidateCacheAfterCommit("刷新淘汰旧代际(refreshAccessToken)", evictions);')

io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('service patch ok')
