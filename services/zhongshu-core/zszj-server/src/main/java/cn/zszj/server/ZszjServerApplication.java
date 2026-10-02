package cn.zszj.server;

import cn.zszj.framework.common.util.date.DateUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * 项目的启动类
 *
 *
 * @author 众墅之家（基于开源底座改造，来源署名见 THIRD_PARTY_NOTICES.md）
 */
@SuppressWarnings("SpringComponentScan") // 忽略 IDEA 无法识别 ${zszj.info.base-package}
@SpringBootApplication(scanBasePackages = {"${zszj.info.base-package}.server", "${zszj.info.base-package}.module"})
public class ZszjServerApplication {

    public static void main(String[] args) {

        // ZS-SEC-009：固定应用默认时区为 GMT+8（接口边界时间合同的部署级基线），使全项目
        // LocalDateTime.now() 生产端与固定时区序列化对齐，不受宿主 JVM/容器时区影响
        // （Dockerfile 已设 ENV TZ=Asia/Shanghai，此处兜底裸 jar / CI / 本地运行）
        TimeZone.setDefault(TimeZone.getTimeZone(DateUtils.TIME_ZONE_DEFAULT));

        SpringApplication.run(ZszjServerApplication.class, args);
//        new SpringApplicationBuilder(ZszjServerApplication.class)
//                .applicationStartup(new BufferingApplicationStartup(20480))
//                .run(args);

    }

}
