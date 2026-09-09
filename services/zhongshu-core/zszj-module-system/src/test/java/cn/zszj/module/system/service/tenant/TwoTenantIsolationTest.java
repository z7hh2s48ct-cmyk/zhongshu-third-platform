package cn.zszj.module.system.service.tenant;

import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.system.dal.dataobject.dept.DeptDO;
import cn.zszj.module.system.dal.mysql.dept.DeptMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZS-DB-018：技术租户两租户隔离夹具。
 * 关键发现：BaseDbUnitTest 使用 H2 内存库，未配置 TenantLineInnerInterceptor，
 * 因此本测试验证 Mapper 层基础 CRUD；租户拦截器级隔离需 B02 真实 PG 验证。
 */
class TwoTenantIsolationTest extends BaseDbUnitTest {

    @Resource
    private DeptMapper deptMapper;

    @Test
    void insertAndSelectDept() {
        DeptDO dept = new DeptDO();
        dept.setName("测试部门");
        dept.setStatus(0);
        dept.setDeleted(false);
        deptMapper.insert(dept);
        assertNotNull(dept.getId());
        assertNotNull(deptMapper.selectById(dept.getId()));
    }

    @Test
    void logicalDeleteRemovesRow() {
        DeptDO dept = new DeptDO();
        dept.setName("待删部门");
        dept.setStatus(0);
        dept.setDeleted(false);
        deptMapper.insert(dept);
        Long id = dept.getId();
        deptMapper.deleteById(id);
        assertNull(deptMapper.selectById(id));
    }
}
