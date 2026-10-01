package cn.zszj.module.firstchain.controller.admin.application.vo;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ApplicationCreateReqVO} 资质附件引用的入参约束（ZS-FC-001 附件写入侧）。
 *
 * <p>业务层的存在性/归属/可读性校验由 {@code FileApi#validatePrivateFileReferences} 承担；本测试只锁定
 * 请求层的结构性约束——元素不得为 null、必须为正数（文件编号），数量不得超过 64 个。
 *
 * @author ZS-FC-001
 */
class ApplicationCreateReqVOValidationTest {

    private static ValidatorFactory factory;

    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void validAttachmentIds_noViolation() {
        assertThat(violations(base(List.of(101L, 102L)))).isEmpty();
    }

    @Test
    void nullAttachmentList_noViolation() {
        assertThat(violations(base(null))).isEmpty();
    }

    @Test
    void nullElement_rejected() {
        assertThat(violations(base(Arrays.asList(101L, null))))
                .anyMatch(v -> v.getPropertyPath().toString().startsWith("attachmentFileIds"));
    }

    @Test
    void nonPositiveElement_rejected() {
        assertThat(violations(base(List.of(0L)))).isNotEmpty();
        assertThat(violations(base(List.of(-5L)))).isNotEmpty();
    }

    @Test
    void moreThan64Attachments_rejected() {
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 65; i++) {
            ids.add(i);
        }
        assertThat(violations(base(ids))).isNotEmpty();
    }

    private static ApplicationCreateReqVO base(List<Long> attachmentFileIds) {
        ApplicationCreateReqVO reqVO = new ApplicationCreateReqVO();
        reqVO.setApplicantName("众墅家装联盟（华东）");
        reqVO.setAttachmentFileIds(attachmentFileIds);
        return reqVO;
    }

    private static Set<ConstraintViolation<ApplicationCreateReqVO>> violations(ApplicationCreateReqVO reqVO) {
        return validator.validate(reqVO);
    }

}
