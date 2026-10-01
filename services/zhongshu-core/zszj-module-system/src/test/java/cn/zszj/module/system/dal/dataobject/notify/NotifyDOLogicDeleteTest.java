package cn.zszj.module.system.dal.dataobject.notify;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MSG 域三表 DO 的逻辑删除字面量合同（boolean {@code deleted} 列 × 全局 {@code 0/1} 字面量的域级方言错配收口）。
 *
 * <p>{@code system_notify_send_log} / {@code system_notify_todo} / {@code system_notify_channel_send} 的
 * {@code deleted} 列均为 {@code boolean}（V20260915.003/.004、V20260916.002），全局 {@code @TableLogic} 为数值
 * {@code 0/1}：MyBatis-Plus 注入的 {@code selectById} 等方法在 PG 拼 {@code deleted = 0} 报
 * {@code operator does not exist: boolean = integer}（H2 的 {@code bit} 接受 0，故单测不暴露）。
 * ZS-MSG-004 评审 r2 P2 登记为 MSG 域系统性问题，仅 {@code NotifyChannelSendDO} 自愈；本测试把三表一并锁住——
 * 字段级 {@code @TableLogic(value = "FALSE", delval = "TRUE")} 覆写优先于全局配置。
 *
 * <p>不依赖数据库：直接断言 MyBatis-Plus 为各 DO 解析出的逻辑删除字面量。
 */
class NotifyDOLogicDeleteTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(classes = {NotifySendLogDO.class, NotifyTodoDO.class, NotifyChannelSendDO.class})
    void logicDeleteLiterals_areBooleanCompatible(Class<?> doClass) {
        TableInfo tableInfo = TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), doClass);

        TableFieldInfo logicDelete = tableInfo.getLogicDeleteFieldInfo();

        assertThat(logicDelete).as("%s 须声明逻辑删除字段", doClass.getSimpleName()).isNotNull();
        assertThat(logicDelete.getColumn()).isEqualTo("deleted");
        assertThat(logicDelete.getLogicNotDeleteValue())
                .as("%s：boolean 列的未删除字面量须为 FALSE，数值 0 在 PG 上 boolean = integer 不成立", doClass.getSimpleName())
                .isEqualToIgnoringCase("FALSE");
        assertThat(logicDelete.getLogicDeleteValue()).isEqualToIgnoringCase("TRUE");
    }

}
