package cn.zszj.framework.common.util.date;

import cn.hutool.core.date.LocalDateTimeUtil;

import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * 时间工具类
 *
 * @author 芋道源码
 */
public class DateUtils {

    /**
     * 时区 - 默认
     */
    public static final String TIME_ZONE_DEFAULT = "GMT+8";

    /**
     * 时区 - 默认 ZoneId
     *
     * ZS-SEC-009：接口边界时间序列化/反序列化的单一事实源，由 {@link #TIME_ZONE_DEFAULT} 派生
     * （GMT+8 固定偏移，等价 Asia/Shanghai，中国无夏令时），替代易受部署 JVM 影响的
     * {@link ZoneId#systemDefault()}，保证同一 LocalDateTime 在任意部署时区下得到一致的 epoch millis。
     * 与 {@code LocalDateTimeUtils#DEFAULT_ZONE_ID} 采用同一派生方式，全项目时区约定唯一。
     */
    public static final ZoneId ZONE_DEFAULT = TimeZone.getTimeZone(TIME_ZONE_DEFAULT).toZoneId();

    /**
     * 秒转换成毫秒
     */
    public static final long SECOND_MILLIS = 1000;

    public static final String FORMAT_YEAR_MONTH_DAY = "yyyy-MM-dd";

    public static final String FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND = "yyyy-MM-dd HH:mm:ss";

    /**
     * 当前时间（固定 {@link #ZONE_DEFAULT} GMT+8）
     *
     * ZS-SEC-009：接口边界时间合同的「生产端」原语。替代 {@link LocalDateTime#now()}（依赖部署 JVM 默认时区），
     * 使被序列化/比较的 LocalDateTime（如令牌过期时间）与固定时区的 TimestampLocalDateTimeSerializer 对齐，
     * 消除 UTC 部署下 creation(systemDefault) 与 serialization(GMT+8) 失配导致的时间偏移。
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_DEFAULT);
    }

    /**
     * 将 LocalDateTime 转换成 Date
     *
     * @param date LocalDateTime
     * @return LocalDateTime
     */
    public static Date of(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        // 将此日期时间与时区相结合以创建 ZonedDateTime
        ZonedDateTime zonedDateTime = date.atZone(ZoneId.systemDefault());
        // 本地时间线 LocalDateTime 到即时时间线 Instant 时间戳
        Instant instant = zonedDateTime.toInstant();
        // UTC时间(世界协调时间,UTC + 00:00)转北京(北京,UTC + 8:00)时间
        return Date.from(instant);
    }

    /**
     * 将 Date 转换成 LocalDateTime
     *
     * @param date Date
     * @return LocalDateTime
     */
    public static LocalDateTime of(Date date) {
        if (date == null) {
            return null;
        }
        // 转为时间戳
        Instant instant = date.toInstant();
        // UTC时间(世界协调时间,UTC + 00:00)转北京(北京,UTC + 8:00)时间
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static Date addTime(Duration duration) {
        return new Date(System.currentTimeMillis() + duration.toMillis());
    }

    public static boolean isExpired(LocalDateTime time) {
        // ZS-SEC-009：用固定时区的 now() 比较，与令牌 expiresTime 的生产端（DateUtils.now()）对齐
        LocalDateTime now = now();
        return now.isAfter(time);
    }

    /**
     * 创建指定时间
     *
     * @param year  年
     * @param month 月
     * @param day   日
     * @return 指定时间
     */
    public static Date buildTime(int year, int month, int day) {
        return buildTime(year, month, day, 0, 0, 0);
    }

    /**
     * 创建指定时间
     *
     * @param year   年
     * @param month  月
     * @param day    日
     * @param hour   小时
     * @param minute 分钟
     * @param second 秒
     * @return 指定时间
     */
    public static Date buildTime(int year, int month, int day,
                                 int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 0); // 一般情况下，都是 0 毫秒
        return calendar.getTime();
    }

    public static Date max(Date a, Date b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.compareTo(b) > 0 ? a : b;
    }

    public static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    /**
     * 是否今天
     *
     * @param date 日期
     * @return 是否
     */
    public static boolean isToday(LocalDateTime date) {
        return LocalDateTimeUtil.isSameDay(date, LocalDateTime.now());
    }

    /**
     * 是否昨天
     *
     * @param date 日期
     * @return 是否
     */
    public static boolean isYesterday(LocalDateTime date) {
        return LocalDateTimeUtil.isSameDay(date, LocalDateTime.now().minusDays(1));
    }

}
