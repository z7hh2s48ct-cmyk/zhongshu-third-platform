package cn.zszj.module.firstchain.dal.dataobject.application;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ApplicationDO} 逻辑删除字面量合同（真实 PG 暴露的方言缺陷回归锚点）。
 *
 * <p>背景：{@code bpm_first_chain_application.deleted} 为 {@code boolean}（V20260928.001，对齐 MSG 域惯例），而全局
 * {@code @TableLogic} 配置为 {@code 0/1} 数值字面量（application.yaml {@code logic-delete-value}）——
 * MyBatis-Plus 的 {@code selectById}/{@code selectPage} 因此拼出 {@code deleted = 0}，PG 上报
 * {@code operator does not exist: boolean = integer}，申请域 {@code GET /firstchain/application/get|page} 返回 500；
 * H2（{@code bit} 接受 0）与手写 JDBC 的 {@code deleted = FALSE} 路径都不暴露，故此前所有单测与 PG 运行期套件均未发现。
 *
 * <p>本测试不依赖数据库：直接断言 MyBatis-Plus 为该 DO 解析出的逻辑删除字面量是 boolean 兼容的
 * {@code FALSE/TRUE}（字段级 {@code @TableLogic} 覆写优先于全局配置，MSG-004 {@code NotifyChannelSendDO} 先例）。
 *
 * @author ZS-FC-001
 */
class ApplicationDOLogicDeleteTest {

    @Test
    void logicDeleteLiterals_areBooleanCompatible() {
        TableInfo tableInfo = TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ApplicationDO.class);

        TableFieldInfo logicDelete = tableInfo.getLogicDeleteFieldInfo();

        assertThat(logicDelete).as("ApplicationDO 须声明逻辑删除字段").isNotNull();
        assertThat(logicDelete.getColumn()).isEqualTo("deleted");
        assertThat(logicDelete.getLogicNotDeleteValue())
                .as("boolean 列的未删除字面量须为 FALSE，数值 0 在 PG 上 boolean = integer 不成立")
                .isEqualToIgnoringCase("FALSE");
        assertThat(logicDelete.getLogicDeleteValue()).isEqualToIgnoringCase("TRUE");
    }

}
