package cn.zszj.module.infra.service.file;

import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportGenerateReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileExportGenerateRespVO;

/**
 * 导出生成通道 Service（ZS-FILE-004.B）。
 *
 * <p>生成-交付两阶段重检的「生成」侧：对导出条件（对象维 org 轴 / visit 收敛 + 字段维声明字段
 * ⊆ 授权字段）执行统一裁决（ZS-PERM-003.A 机制）后落盘私有导出件；未接入对象类型 fail-closed。
 * 内部 Java API（无 HTTP 端点），内容由服务端业务代码传入；裁剪由调用方消费裁决输出实现。</p>
 *
 * @author ZS-FILE-004.B
 */
public interface FileExportDeliveryService {

    /**
     * 生成导出件（重检 → 落盘私有资产）。
     *
     * @param req       导出生成条件与内容
     * @param loginUser 登录主体（无登录主体拒绝生成）
     * @return 导出件编号与保留期
     */
    FileExportGenerateRespVO generateExportFile(FileExportGenerateReqVO req, LoginUser loginUser);

}
