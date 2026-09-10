package cn.zszj.framework.web.core.filter;

import cn.zszj.framework.common.util.servlet.ServletUtils;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;

/**
 *  Request Body 缓存 Wrapper
 *
 * @author 芋道源码
 */
public class CacheRequestBodyWrapper extends HttpServletRequestWrapper {

    /**
     * 缓存的内容
     */
    private final byte[] body;

    public CacheRequestBodyWrapper(HttpServletRequest request) {
        this(request, -1L); // 向后兼容：无参上限不施加额外大小限制
    }

    /**
     * @param maxCacheSize JSON 请求体缓冲上限（字节），{@code <= 0} 表示不限制；{@code > 0} 时边读边累计，
     *                     实际字节超上限抛 {@link TooLargeException}，覆盖 chunked / 未知长度（Content-Length = -1）
     *                     绕过声明式早拒的场景（ZS-SEC-008 codex P1 修复）。
     */
    public CacheRequestBodyWrapper(HttpServletRequest request, long maxCacheSize) {
        super(request);
        body = readBody(request, maxCacheSize);
    }

    /**
     * 读取请求体字节，与 {@link ServletUtils#getBodyBytes} 语义一致（仅 JSON 请求缓存 body）。
     * {@code maxCacheSize <= 0} 沿用原全量读取；{@code > 0} 限界读取，累计超上限立即拒绝，
     * 防止认证前无界缓冲耗尽堆内存。
     */
    private static byte[] readBody(HttpServletRequest request, long maxCacheSize) {
        if (!ServletUtils.isJsonRequest(request)) {
            return null;
        }
        if (maxCacheSize <= 0) {
            return ServletUtils.getBodyBytes(request);
        }
        try (ServletInputStream in = request.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int len;
            while ((len = in.read(buffer)) != -1) {
                total += len;
                if (total > maxCacheSize) {
                    throw new TooLargeException(maxCacheSize);
                }
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 请求体超过缓冲上限时抛出（ZS-SEC-008：限界读取，覆盖 chunked / 未知长度绕过声明式早拒）。
     */
    public static class TooLargeException extends RuntimeException {

        public TooLargeException(long maxCacheSize) {
            super("请求体大小超过上限 " + maxCacheSize + " 字节");
        }

    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(this.getInputStream()));
    }

    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
        // 返回 ServletInputStream
        return new ServletInputStream() {

            @Override
            public int read() {
                return inputStream.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {}

            @Override
            public int available() {
                return body.length;
            }

        };
    }

}
