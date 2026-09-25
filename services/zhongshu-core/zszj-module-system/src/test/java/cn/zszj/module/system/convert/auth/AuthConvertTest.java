package cn.zszj.module.system.convert.auth;

import cn.zszj.framework.common.catalog.ModuleCatalog;
import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zszj.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * ZS-CLIENT-002.B：{@link AuthConvert#convert} 的单元测试——登录权限信息须携带服务端启用模块清单
 * （{@link ModuleCatalog#ENABLED_MODULES}），驱动客户端「关闭模块提供不可用落点」（r2-P2-2：停用模块页
 * 不得放行撞服务端 501）。客户端不自行推导模块清单，只做服务端下发集合的成员判断。
 *
 * <p>转换器为无状态纯函数（MapStruct default 方法），空角色 / 空菜单路径（无角色用户）同样须携带模块清单。
 *
 * @author ZS-CLIENT-002.B
 */
public class AuthConvertTest extends BaseMockitoUnitTest {

    @Test
    public void testConvert_carriesEnabledModules() {
        AdminUserDO user = new AdminUserDO();
        user.setId(1L);
        user.setNickname("测试用户");

        AuthPermissionInfoRespVO resp =
                AuthConvert.INSTANCE.convert(user, Collections.emptyList(), Collections.emptyList());

        assertNotNull(resp);
        assertEquals(ModuleCatalog.ENABLED_MODULES, resp.getEnabledModules());
    }

}
