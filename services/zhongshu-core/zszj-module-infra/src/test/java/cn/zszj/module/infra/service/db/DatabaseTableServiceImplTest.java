package cn.zszj.module.infra.service.db;

import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.dal.dataobject.db.DataSourceConfigDO;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import jakarta.annotation.Resource;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static cn.zszj.framework.test.core.util.RandomUtils.randomLongId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Import(DatabaseTableServiceImpl.class)
public class DatabaseTableServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DatabaseTableServiceImpl databaseTableService;

    @MockitoBean
    private DataSourceConfigService dataSourceConfigService;

    @Test
    public void testGetTableList() {
        // 准备参数
        Long dataSourceConfigId = randomLongId();
        // mock 方法
        DataSourceConfigDO dataSourceConfig = new DataSourceConfigDO().setUsername("sa").setPassword("")
                .setUrl("jdbc:h2:mem:testdb");
        when(dataSourceConfigService.getDataSourceConfig(eq(dataSourceConfigId)))
                .thenReturn(dataSourceConfig);

        // 调用
        List<TableInfo> tables = databaseTableService.getTableList(dataSourceConfigId,
                "config", "参数");
        // 断言：ZS-CFG-004 B04 新增 infra_config_history（参数配置变更历史表）同含 config 关键字与「参数」注释，
        // 与 infra_config 一并命中
        assertEquals(2, tables.size());
        TableInfo configTable = tables.stream().filter(table -> "infra_config".equals(table.getName()))
                .findFirst().orElseThrow();
        assertTableInfo(configTable);
    }

    @Test
    public void testGetTable() {
        // 准备参数
        Long dataSourceConfigId = randomLongId();
        // mock 方法
        DataSourceConfigDO dataSourceConfig = new DataSourceConfigDO().setUsername("sa").setPassword("")
                .setUrl("jdbc:h2:mem:testdb");
        when(dataSourceConfigService.getDataSourceConfig(eq(dataSourceConfigId)))
                .thenReturn(dataSourceConfig);

        // 调用
        TableInfo tableInfo = databaseTableService.getTable(dataSourceConfigId, "infra_config");
        // 断言
        assertTableInfo(tableInfo);
    }

    private void assertTableInfo(TableInfo tableInfo) {
        assertEquals("infra_config", tableInfo.getName());
        assertEquals("参数配置表", tableInfo.getComment());
        // codex 前欠账补同步：CFG-004 为 infra_config 新增 version 列（13→14）；
        // ZS-FILE-001.A 的 infra_file 列变化不影响本断言
        assertEquals(14, tableInfo.getFields().size());
        // id 字段
        TableField idField = tableInfo.getFields().get(0);
        assertEquals("id", idField.getName());
        assertEquals(JdbcType.BIGINT, idField.getMetaInfo().getJdbcType());
        assertEquals("编号", idField.getComment());
        assertFalse(idField.getMetaInfo().isNullable());
        assertTrue(idField.isKeyFlag());
        assertTrue(idField.isKeyIdentityFlag());
        assertEquals(DbColumnType.LONG, idField.getColumnType());
        assertEquals("id", idField.getPropertyName());
        // name 字段
        TableField nameField = tableInfo.getFields().get(3);
        assertEquals("name", nameField.getName());
        assertEquals(JdbcType.VARCHAR, nameField.getMetaInfo().getJdbcType());
        assertEquals("名字", nameField.getComment());
        assertFalse(nameField.getMetaInfo().isNullable());
        assertFalse(nameField.isKeyFlag());
        assertFalse(nameField.isKeyIdentityFlag());
        assertEquals(DbColumnType.STRING, nameField.getColumnType());
        assertEquals("name", nameField.getPropertyName());
    }
}
