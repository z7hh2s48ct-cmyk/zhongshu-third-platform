package cn.zszj.module.infra.enums.file;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文件可见范围枚举（ZS-FILE-001.A）
 *
 * @author ZS-FILE-001.A
 */
@RequiredArgsConstructor
@Getter
public enum FileScopeEnum {

    /** 公开素材：匿名可读（批准用途） */
    PUBLIC("PUBLIC"),

    /** 私有附件：需登录且同技术租户（默认） */
    PRIVATE("PRIVATE");

    private final String scope;

    /** 校验给定 scope 是否为合法枚举值 */
    public static boolean isValid(String scope) {
        for (FileScopeEnum e : values()) {
            if (e.scope.equals(scope)) {
                return true;
            }
        }
        return false;
    }

}
