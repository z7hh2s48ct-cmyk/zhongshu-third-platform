package cn.zszj.module.system.service.sms;

import cn.zszj.module.system.api.sms.dto.code.SmsCodeValidateReqDTO;
import cn.zszj.module.system.dal.dataobject.sms.SmsCodeDO;
import cn.zszj.module.system.dal.mysql.sms.SmsCodeMapper;
import cn.zszj.module.system.dal.redis.sms.SmsCodeSecurityRedisDAO;
import cn.zszj.module.system.dal.redis.sms.SmsCodeSecurityRedisDAO.ValidateAttemptReservation;
import cn.zszj.module.system.framework.sms.config.SmsCodeProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ZS-LOGIN-004 P2（codex r1，DB 异常释放）：当校验时 {@code selectLastByMobile} 抛 DB 连接/查询超时异常，
 * 刚预留的尝试名额必须被释放——因为并未「确认码不匹配」。
 *
 * <p>缺陷：r0 只在「码不存在/过期/已使用/成功」路径释放，DB 查询<b>异常</b>时预留名额永不释放（异常直接冒泡）。
 * 用户用正确码重试若干次即耗尽默认上限、DB 恢复后仍被锁死。
 *
 * <p>修复：{@code validateSmsCode0} 用 try/catch 包裹 {@code selectLastByMobile}，异常路径也调
 * {@code releaseValidateAttempt(reservation)} 后原样抛出；<b>仅</b>对确认的「码不匹配」（{@code lastSmsCode == null}）保留预留。
 *
 * <p>RED（对着 r0）：DB 异常时 {@code releaseValidateAttempt} 从不被调用 → {@code Wanted but not invoked} 断言失败。
 *
 * <p>本类为纯单元测试：裸 {@code new SmsCodeServiceImpl()} + mock(Mapper/DAO) + {@link ReflectionTestUtils#setField}，
 * 绕开 Spring CGLIB 代理，直接验证释放生命周期。
 *
 * @author ZS-LOGIN-004 P2(r1) DB 异常释放
 */
public class SmsCodeServiceImplValidateDbFailureReleaseTest {

    private static final String MOBILE = "15690000501";
    private static final Integer SCENE = 1;
    private static final String CODE = "1234";
    private static final Duration LOCK = Duration.ofMinutes(10);

    private SmsCodeServiceImpl newService(SmsCodeProperties properties, SmsCodeMapper mapper,
                                          SmsSendService sendService, SmsCodeSecurityRedisDAO dao) {
        SmsCodeServiceImpl service = new SmsCodeServiceImpl();
        ReflectionTestUtils.setField(service, "smsCodeProperties", properties);
        ReflectionTestUtils.setField(service, "smsCodeMapper", mapper);
        ReflectionTestUtils.setField(service, "smsSendService", sendService);
        ReflectionTestUtils.setField(service, "smsCodeSecurityRedisDAO", dao);
        return service;
    }

    private SmsCodeProperties mockProperties() {
        SmsCodeProperties properties = mock(SmsCodeProperties.class);
        when(properties.getMaxValidateAttempts()).thenReturn(5);
        when(properties.getAttemptLockDuration()).thenReturn(LOCK);
        when(properties.getExpireTimes()).thenReturn(Duration.ofMinutes(5));
        return properties;
    }

    private SmsCodeValidateReqDTO reqDTO() {
        SmsCodeValidateReqDTO reqDTO = new SmsCodeValidateReqDTO();
        reqDTO.setMobile(MOBILE);
        reqDTO.setCode(CODE);
        reqDTO.setScene(SCENE);
        return reqDTO;
    }

    @Test
    public void dbQueryFailure_releasesReservedAttempt_andRethrows() {
        SmsCodeProperties properties = mockProperties();
        SmsCodeMapper mapper = mock(SmsCodeMapper.class);
        SmsSendService sendService = mock(SmsSendService.class);
        SmsCodeSecurityRedisDAO dao = mock(SmsCodeSecurityRedisDAO.class);
        ValidateAttemptReservation reservation = ValidateAttemptReservation.reserved(MOBILE, SCENE, 1L, "tok-dbex");
        when(dao.reserveValidateAttempt(eq(MOBILE), eq(SCENE), eq(5), any())).thenReturn(reservation);
        when(mapper.selectLastByMobile(MOBILE, CODE, SCENE))
                .thenThrow(new RuntimeException("simulated DB connection/query timeout"));

        SmsCodeServiceImpl service = newService(properties, mapper, sendService, dao);

        // DB 异常原样冒泡
        assertThrows(RuntimeException.class, () -> service.validateSmsCode(reqDTO()));
        // 关键：异常路径必须释放刚预留的名额（r0 从不释放 → 此 verify 失败）
        verify(dao).releaseValidateAttempt(reservation);
    }

    @Test
    public void confirmedCodeMismatch_keepsReservation_doesNotRelease() {
        SmsCodeProperties properties = mockProperties();
        SmsCodeMapper mapper = mock(SmsCodeMapper.class);
        SmsSendService sendService = mock(SmsSendService.class);
        SmsCodeSecurityRedisDAO dao = mock(SmsCodeSecurityRedisDAO.class);
        ValidateAttemptReservation reservation = ValidateAttemptReservation.reserved(MOBILE, SCENE, 1L, "tok-mismatch");
        when(dao.reserveValidateAttempt(eq(MOBILE), eq(SCENE), eq(5), any())).thenReturn(reservation);
        // 码不匹配：查询正常返回 null（确认的暴力尝试信号）
        when(mapper.selectLastByMobile(MOBILE, CODE, SCENE)).thenReturn(null);

        SmsCodeServiceImpl service = newService(properties, mapper, sendService, dao);

        assertThrows(RuntimeException.class, () -> service.validateSmsCode(reqDTO()));
        // 仅对确认的码不匹配保留预留：不得释放（该名额即为本次失败计数）
        verify(dao, never()).releaseValidateAttempt(any());
    }

    @Test
    public void dbRecoversThenCorrectCode_succeeds_andReleases() {
        SmsCodeProperties properties = mockProperties();
        SmsCodeMapper mapper = mock(SmsCodeMapper.class);
        SmsSendService sendService = mock(SmsSendService.class);
        SmsCodeSecurityRedisDAO dao = mock(SmsCodeSecurityRedisDAO.class);
        ValidateAttemptReservation reservation = ValidateAttemptReservation.reserved(MOBILE, SCENE, 1L, "tok-recover");
        when(dao.reserveValidateAttempt(eq(MOBILE), eq(SCENE), eq(5), any())).thenReturn(reservation);
        // DB 恢复后返回一个有效（未过期、未使用）的验证码
        SmsCodeDO valid = SmsCodeDO.builder().id(1L).mobile(MOBILE).code(CODE).scene(SCENE).used(false).build();
        valid.setCreateTime(LocalDateTime.now());
        when(mapper.selectLastByMobile(MOBILE, CODE, SCENE)).thenReturn(valid);

        SmsCodeServiceImpl service = newService(properties, mapper, sendService, dao);

        // 校验通过、不被锁死，且成功路径释放预留名额
        assertDoesNotThrow(() -> service.validateSmsCode(reqDTO()));
        verify(dao).releaseValidateAttempt(reservation);
    }
}
