package cn.zszj.module.system.dal.mysql.sms;

import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.QueryWrapperX;
import cn.zszj.module.system.dal.dataobject.sms.SmsCodeDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface SmsCodeMapper extends BaseMapperX<SmsCodeDO> {

    /**
     * 获得手机号的最后一个手机验证码
     *
     * @param mobile 手机号
     * @param scene 发送场景，选填
     * @param code 验证码 选填
     * @return 手机验证码
     */
    default SmsCodeDO selectLastByMobile(String mobile, String code, Integer scene) {
        return selectOne(new QueryWrapperX<SmsCodeDO>()
                .eq("mobile", mobile)
                .eqIfPresent("scene", scene)
                .eqIfPresent("code", code)
                .orderByDesc("id")
                .limitN(1));
    }

    /**
     * ZS-LOGIN-004：以「条件 UPDATE」原子消费（领取）一条验证码。
     * <p>
     * 生成的 SQL 形如：
     * <pre>
     * UPDATE system_sms_code SET used = ?, used_time = ?, used_ip = ?
     *  WHERE id = ? AND used = false AND deleted = 0
     * </pre>
     * 关键点在于把 {@code used = false} 放进 <b>WHERE</b> 而不是先 SELECT 再 UPDATE：
     * 数据库对同一行的 UPDATE 天然互斥，因此 N 个线程并发消费同一条验证码时，
     * <b>有且仅有一个</b>能拿到影响行数 1，其余全部拿到 0，从而把「一次性消费」的保证下沉到存储层，
     * 不依赖应用层 check-then-act 的时序（后者在并发下会放过多个消费者）。
     * <p>
     * H2 与 PostgreSQL 在「单行 UPDATE 的原子性 + 影响行数语义」上是一致的，故单测可在 H2 下真实复现串行化效果；
     * 这也避免了 {@code SELECT ... FOR UPDATE} 在两库之间锁语义差异带来的不确定性。
     *
     * @param id       验证码编号
     * @param usedTime 使用时间
     * @param usedIp   使用 IP
     * @return 影响行数：1 = 本次调用成功领取；0 = 已被其它调用消费（或记录不存在/已删除）
     */
    default int consumeById(Long id, LocalDateTime usedTime, String usedIp) {
        SmsCodeDO updateObj = new SmsCodeDO()
                .setUsed(true)
                .setUsedTime(usedTime)
                .setUsedIp(usedIp);
        return update(updateObj, new LambdaUpdateWrapper<SmsCodeDO>()
                .eq(SmsCodeDO::getId, id)
                .eq(SmsCodeDO::getUsed, false)); // CAS 条件：只有「尚未被消费」才允许领取
    }

}
