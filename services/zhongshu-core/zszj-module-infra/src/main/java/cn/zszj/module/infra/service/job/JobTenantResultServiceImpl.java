package cn.zszj.module.infra.service.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.biz.infra.job.dto.TenantJobExecutionResult;
import cn.zszj.module.infra.dal.dataobject.job.JobTenantResultDO;
import cn.zszj.module.infra.dal.mysql.job.JobTenantResultMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 定时任务租户级执行结果 Service 实现类（ZS-JOB-001）
 *
 * 落库前对摘要做两件事，且顺序不可颠倒：
 * 1. 脱敏：异常根因里常带口令/令牌，先按"键=值"结构把值替换为 {@value #MASK}；
 * 2. 截断：再截到 {@link #SUMMARY_MAX_LENGTH} 字符，与 DB 列宽对齐。
 * 先截断再脱敏会让密钥被截成半截后残留在库里，因此必须先脱敏。
 */
@Service
@Validated
@Slf4j
public class JobTenantResultServiceImpl implements JobTenantResultService {

    /**
     * 摘要最大长度，与 infra_job_tenant_result 的 result_summary / error_summary 列宽一致
     */
    static final int SUMMARY_MAX_LENGTH = 512;
    /**
     * 脱敏占位符
     */
    static final String MASK = "***";
    /**
     * 敏感键名。键与值两侧都允许可选引号，用于覆盖 JSON 形式的 {@code {"password":"secret"}}
     */
    private static final String SENSITIVE_KEYS =
            "password|passwd|pwd|secret|token|access[-_]?key|api[-_]?key|private[-_]?key|authorization";
    /**
     * 敏感键 + 分隔符 + 可选开引号。
     *
     * codex r2 指出：r1 的交替分支用 [^"'\\]* 同时排除两种引号，导致混合引号
     * （如 {"password":"alpha'beta"}）整体不匹配，凭证完全泄露；且嵌套量词
     * (?:\\.[^"'\\]*)* 在长值（5000+ 转义）时触发 StackOverflowError。
     *
     * 改为只匹配"键 + 分隔符 + 开引号"，值的边界由 {@link #summarize} 迭代扫描确定，
     * 彻底消除正则递归，同时按开引号类型分别处理闭合（允许值内出现另一种引号）。
     */
    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile(
            "(?i)([\"']?(?:" + SENSITIVE_KEYS + ")[\"']?)(\\s*[=:]\\s*)([\"']?)");
    /**
     * 用于非引号值扫描时判断"下一个词是否是新的敏感键"，避免吃穿到后续键值对
     */
    private static final Pattern NEXT_KEY_PATTERN = Pattern.compile(
            "(?i)[\"']?(?:" + SENSITIVE_KEYS + ")[\"']?\\s*[=:]");
    /**
     * 单独出现的凭证前缀（没有敏感键名时也要脱敏），例如异常栈里直接打印的 {@code Bearer eyJhbGciOi...}
     */
    private static final Pattern CREDENTIAL_PATTERN = Pattern.compile(
            "(?i)\\b(bearer|basic)[ \\t]+[A-Za-z0-9\\-._~+/]+=*");

    @Resource
    private JobTenantResultMapper jobTenantResultMapper;

    @Override
    @Async
    public void saveTenantResultsAsync(Long jobLogId, TenantJobExecutionResult result) {
        if (jobLogId == null || result == null || CollUtil.isEmpty(result.getPerTenantResults())) {
            return;
        }
        try {
            List<JobTenantResultDO> details = new ArrayList<>(result.getPerTenantResults().size());
            for (Map.Entry<Long, TenantJobExecutionResult.TenantItem> entry : result.getPerTenantResults().entrySet()) {
                TenantJobExecutionResult.TenantItem item = entry.getValue();
                if (item == null) {
                    continue;
                }
                details.add(JobTenantResultDO.builder()
                        .jobLogId(jobLogId)
                        .tenantId(entry.getKey())
                        .success(item.isSuccess())
                        .durationMs(item.getDurationMs())
                        .resultSummary(summarize(item.getResult()))
                        .errorSummary(summarize(item.getError()))
                        .build());
            }
            if (CollUtil.isEmpty(details)) {
                return;
            }
            jobTenantResultMapper.insertBatch(details);
        } catch (Exception ex) {
            // 明细落库失败不能影响任务本身：执行日志中仍保留完整的结构化结果
            log.error("[saveTenantResultsAsync][jobLogId({}) 记录租户级执行明细失败]", jobLogId, ex);
        }
    }

    @Override
    public List<JobTenantResultDO> getJobTenantResultList(Long jobLogId) {
        return jobTenantResultMapper.selectListByJobLogId(jobLogId);
    }

    @Override
    public Long getJobTenantFailureCount(Long jobLogId) {
        return jobTenantResultMapper.selectCountByJobLogIdAndSuccess(jobLogId, false);
    }

    /**
     * 先脱敏再截断，产出可安全入库、可安全回显的摘要
     *
     * 脱敏分两步，且顺序不可颠倒：先处理单独的凭证前缀，再处理敏感键值对。
     * 颠倒的话，键值对规则会先把 {@code Bearer} 当成值替换掉，留下后面的真实凭证。
     *
     * 敏感键值对的值边界由迭代扫描确定（不使用嵌套量词正则），确保：
     * 1. 混合引号（值内含另一种引号）正确匹配到闭合引号（codex r2 [P1]）；
     * 2. 超长值（5000+ 转义/词）不会触发 StackOverflowError（codex r2 [P2]）。
     *
     * @param text 原始文本
     * @return 摘要，入参为空时返回空串
     */
    private static String summarize(String text) {
        if (StrUtil.isBlank(text)) {
            return StrUtil.EMPTY;
        }
        String masked = CREDENTIAL_PATTERN.matcher(text).replaceAll("$1 " + MASK);
        StringBuilder sb = new StringBuilder();
        Matcher m = SENSITIVE_KEY_PATTERN.matcher(masked);
        int lastEnd = 0;
        while (m.find(lastEnd)) {
            sb.append(masked, lastEnd, m.start());
            String key = m.group(1);
            String sep = m.group(2);
            String openQuote = m.group(3);
            int valueStart = m.end();
            int valueEnd;
            if (!openQuote.isEmpty()) {
                // 引号值：迭代扫描到闭合引号（处理 \ 转义），允许值内出现另一种引号
                valueEnd = scanQuotedValueEnd(masked, valueStart, openQuote.charAt(0));
                sb.append(key).append(sep).append(openQuote).append(MASK).append(openQuote);
            } else {
                // 非引号值：迭代扫描到空白/引号/下一个敏感键
                valueEnd = scanUnquotedValueEnd(masked, valueStart);
                if (valueEnd == valueStart) {
                    // 无值字符（如 "password=" 后面为空），不脱敏，原样保留
                    sb.append(key).append(sep);
                    lastEnd = valueStart;
                    continue;
                }
                sb.append(key).append(sep).append(MASK);
            }
            lastEnd = valueEnd;
        }
        sb.append(masked, lastEnd, masked.length());
        masked = sb.toString();
        return masked.length() <= SUMMARY_MAX_LENGTH ? masked : masked.substring(0, SUMMARY_MAX_LENGTH);
    }

    /**
     * 从 start 开始迭代扫描引号值，返回闭合引号之后的位置。
     * 处理 \ 转义（跳过转义字符），只匹配与 openQuote 相同的闭合引号，
     * 因此值内可以安全包含另一种引号（codex r2 [P1] 混合引号场景）。
     * 未闭合时返回文本末尾（保守策略：宁可多 mask 不可泄露）。
     */
    private static int scanQuotedValueEnd(String text, int start, char openQuote) {
        int i = start;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == openQuote) {
                return i + 1;
            }
            i++;
        }
        return text.length();
    }

    /**
     * 从 start 开始迭代扫描非引号值，返回值末尾位置。
     * 值由非空白、非引号字符组成，允许词间空格/制表符，
     * 但遇到下一个敏感键时停止（避免吃穿到后续键值对）。
     */
    private static int scanUnquotedValueEnd(String text, int start) {
        int i = start;
        int lastNonWs = start;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '"' || c == '\'') {
                break;
            }
            // codex r4 [P1] fix: use isSpaceLike to cover ALL Unicode space characters
            // (isWhitespace misses NBSP/FIGURE SPACE; isSpaceChar misses EM SPACE in some contexts)
            if (isSpaceLike(c)) {
                int j = i;
                while (j < text.length() && isSpaceLike(text.charAt(j))) {
                    j++;
                }
                if (isSensitiveKeyAt(text, j)) {
                    break;
                }
                // codex r4 [P1] fix: if whitespace leads to a quote, consume the quoted value
                // (SENSITIVE_KEY_PATTERN\'s \s* does not match Unicode whitespace like EM SPACE,
                //  so openQuote is empty and we enter unquoted path despite a quoted value following)
                // codex r5 [P1] guard: only consume quote as value when no content scanned yet
                // (prevents eating adjacent quoted KEY like "token"="bravo")
                if (lastNonWs == start && j < text.length()
                        && (text.charAt(j) == '"' || text.charAt(j) == '\'')) {
                    return scanQuotedValueEnd(text, j + 1, text.charAt(j));
                }
                i = j;
                continue;
            }
            // codex r4 [P1] fix: delimiter lookahead uses isSpaceLike for broader coverage
            if (c == ',' || c == ';' || c == '&') {
                int j = i + 1;
                while (j < text.length() && isSpaceLike(text.charAt(j))) {
                    j++;
                }
                if (isSensitiveKeyAt(text, j)) {
                    break;
                }
            }
            i++;
            lastNonWs = i;
        }
        return lastNonWs;
    }

    /**
     * 判断 text[pos..] 是否以敏感键 + 分隔符开头
     */
    /**
     * 判断字符是否为“空白类”字符，覆盖所有 Unicode 空白/零宽字符。
     * <ul>
     *   <li>{@link Character#isWhitespace} — 标准空白 + EM SPACE + IDEOGRAPHIC SPACE 等</li>
     *   <li>{@link Character#isSpaceChar} — NBSP (U+00A0) + FIGURE SPACE (U+2007) + NARROW NBSP (U+202F)</li>
     *   <li>U+200B ZWSP — 零宽空格（Java 两个 API 均不覆盖）</li>
     *   <li>U+FEFF BOM/ZWNBSP — 字节序标记/零宽不断空格</li>
     * </ul>
     */
    private static boolean isSpaceLike(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c)
                || c == '\u200B' || c == '\uFEFF';
    }

    private static boolean isSensitiveKeyAt(String text, int pos) {
        if (pos >= text.length()) {
            return false;
        }
        Matcher m = NEXT_KEY_PATTERN.matcher(text);
        m.region(pos, text.length());
        return m.lookingAt();
    }

}
