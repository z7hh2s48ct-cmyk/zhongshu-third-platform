package cn.zszj.framework.common.util.json.databind;

import cn.zszj.framework.common.util.date.DateUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 基于时间戳的 LocalDateTime 反序列化器
 *
 * @author 老五
 */
public class TimestampLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    public static final TimestampLocalDateTimeDeserializer INSTANCE = new TimestampLocalDateTimeDeserializer();

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // 将 Long 时间戳，转换为 LocalDateTime 对象
        // ZS-SEC-009：使用固定时区 DateUtils.ZONE_DEFAULT（GMT+8）替代 ZoneId.systemDefault()，与序列化器对称
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(p.getValueAsLong()), DateUtils.ZONE_DEFAULT);
    }

}
