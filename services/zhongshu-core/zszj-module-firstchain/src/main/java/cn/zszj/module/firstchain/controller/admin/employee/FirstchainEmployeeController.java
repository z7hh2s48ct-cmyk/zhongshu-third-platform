package cn.zszj.module.firstchain.controller.admin.employee;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.module.firstchain.controller.admin.employee.vo.EmployeeCreateReqVO;
import cn.zszj.module.firstchain.controller.admin.employee.vo.EmployeeCreatedRespVO;
import cn.zszj.module.firstchain.controller.admin.employee.vo.EmployeeMemberRespVO;
import cn.zszj.module.firstchain.service.employee.FirstchainEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.zszj.framework.common.pojo.CommonResult.success;

/**
 * 首链员工账号管理端 Controller（ZS-FC-001 员工授权 wave；D-07 M5-A REST 面）。
 *
 * <p>动作级权限串 {@code @PreAuthorize("@ss.hasPermission('firstchain:employee:xxx')")}
 * （接入合同 §1.6；对象级资格〔操作人=FRANCHISEE 组织负责人〕由 {@link FirstchainEmployeeService}
 * 服务层二次校验，两层不可互相替代）。创建操作人取登录上下文，不接受请求体声明。
 *
 * @author ZS-FC-001
 */
@Tag(name = "管理后台 - 首链员工账号")
@RestController
@RequestMapping("/firstchain/employee")
public class FirstchainEmployeeController {

    @Resource
    private FirstchainEmployeeService employeeService;

    @PostMapping("/create")
    @Operation(summary = "创建员工账号（负责人直接创建；返回一次性初始密码）")
    @PreAuthorize("@ss.hasPermission('firstchain:employee:create')")
    public CommonResult<EmployeeCreatedRespVO> createEmployee(@Valid @RequestBody EmployeeCreateReqVO createReqVO) {
        return success(employeeService.createEmployee(createReqVO));
    }

    @GetMapping("/list-members")
    @Operation(summary = "获得本组织在职成员列表（分配/改派员工选择器数据源，ZS-FC-003）")
    @PreAuthorize("@ss.hasPermission('firstchain:employee:list')")
    public CommonResult<List<EmployeeMemberRespVO>> listOrgMembers() {
        return success(employeeService.listOrgMembers());
    }

}
