package cn.zszj.module.infra.enums;

import cn.zszj.framework.common.exception.ErrorCode;

/**
 * Infra 错误码枚举类
 *
 * infra 系统，使用 1-001-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 参数配置 1-001-000-000 ==========
    ErrorCode CONFIG_NOT_EXISTS = new ErrorCode(1_001_000_001, "参数配置不存在");
    ErrorCode CONFIG_KEY_DUPLICATE = new ErrorCode(1_001_000_002, "参数配置 key 重复");
    ErrorCode CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE = new ErrorCode(1_001_000_003, "不能删除类型为系统内置的参数配置");
    ErrorCode CONFIG_GET_VALUE_ERROR_IF_VISIBLE = new ErrorCode(1_001_000_004, "获取参数配置失败，原因：不允许获取不可见配置");
    ErrorCode CONFIG_SENSITIVE_CAN_NOT_SET_VISIBLE = new ErrorCode(1_001_000_005, "敏感/秘密参数不允许设置为可见");
    ErrorCode CONFIG_SENSITIVE_CAN_NOT_DOWNGRADE_ON_MASKED_ECHO = new ErrorCode(1_001_000_006, "敏感/秘密参数在值未变更（回显掩码 ******）时，不允许降低其保护级别（改名为非秘密键或设置为可见）；如需调整请重新填写真实值");
    // ZS-CFG-004 B03：配置值校验错误码
    ErrorCode CONFIG_VALUE_TYPE_MISMATCH = new ErrorCode(1_001_000_007, "参数值类型不匹配，期望类型：{}");
    ErrorCode CONFIG_VALUE_OUT_OF_RANGE = new ErrorCode(1_001_000_008, "参数值超出允许范围 [{}, {}]");
    ErrorCode CONFIG_VALUE_NOT_IN_ALLOWED_SET = new ErrorCode(1_001_000_009, "参数值不在允许的值集 {} 中");
    ErrorCode CONFIG_UPDATE_CONFLICT = new ErrorCode(1_001_000_010, "参数配置更新冲突，请刷新后重试");
    // ZS-CFG-004 B04：配置变更历史与恢复流程错误码
    ErrorCode CONFIG_RESTORE_HISTORY_NOT_EXISTS = new ErrorCode(1_001_000_011, "配置变更历史不存在");
    ErrorCode CONFIG_RESTORE_NOT_RESTORABLE = new ErrorCode(1_001_000_012, "该历史记录不可自动恢复：秘密/敏感参数的历史值已脱敏或该记录类型不支持恢复，请手工重新填写参数值");
    ErrorCode CONFIG_RESTORE_HISTORY_MISMATCH = new ErrorCode(1_001_000_013, "恢复目标历史与参数配置不匹配");

    // ========== 定时任务 1-001-001-000 ==========
    ErrorCode JOB_NOT_EXISTS = new ErrorCode(1_001_001_000, "定时任务不存在");
    ErrorCode JOB_HANDLER_EXISTS = new ErrorCode(1_001_001_001, "定时任务的处理器已经存在");
    ErrorCode JOB_CHANGE_STATUS_INVALID = new ErrorCode(1_001_001_002, "只允许修改为开启或者关闭状态");
    ErrorCode JOB_CHANGE_STATUS_EQUALS = new ErrorCode(1_001_001_003, "定时任务已经处于该状态，无需修改");
    ErrorCode JOB_UPDATE_ONLY_NORMAL_STATUS = new ErrorCode(1_001_001_004, "只有开启状态的任务，才可以修改");
    ErrorCode JOB_CRON_EXPRESSION_VALID = new ErrorCode(1_001_001_005, "CRON 表达式不正确");
    ErrorCode JOB_HANDLER_BEAN_NOT_EXISTS = new ErrorCode(1_001_001_006, "定时任务的处理器 Bean 不存在，注意 Bean 默认首字母小写");
    ErrorCode JOB_HANDLER_BEAN_TYPE_ERROR = new ErrorCode(1_001_001_007, "定时任务的处理器 Bean 类型不正确，未实现 JobHandler 接口");

    // ========== API 错误日志 1-001-002-000 ==========
    ErrorCode API_ERROR_LOG_NOT_FOUND = new ErrorCode(1_001_002_000, "API 错误日志不存在");
    ErrorCode API_ERROR_LOG_PROCESSED = new ErrorCode(1_001_002_001, "API 错误日志已处理");

    // ========= 文件相关 1-001-003-000 =================
    ErrorCode FILE_PATH_EXISTS = new ErrorCode(1_001_003_000, "文件路径已存在");
    ErrorCode FILE_NOT_EXISTS = new ErrorCode(1_001_003_001, "文件不存在");
    // ZS-FILE-001.A：文件可见范围非法
    // ZS-FILE-002：上传用途、内容校验与唯一对象键
    ErrorCode FILE_SIZE_EXCEED = new ErrorCode(1_001_003_006, "文件大小（{}）超过上限（{}）");
    ErrorCode FILE_TYPE_MISMATCH = new ErrorCode(1_001_003_007, "文件内容与扩展名不符：探测类型（{}）与扩展名（{}）不一致，疑似伪装");
    ErrorCode FILE_DANGEROUS_CONTENT = new ErrorCode(1_001_003_008, "文件扩展名（{}）属于危险类型，禁止上传");
    ErrorCode FILE_UPLOAD_CONCURRENT_LIMIT = new ErrorCode(1_001_003_010, "上传并发已达上限，请稍后重试");
    // ZS-FILE-003：预签名直传凭证
    ErrorCode FILE_UPLOAD_CREDENTIAL_NOT_EXISTS = new ErrorCode(1_001_003_020, "上传凭证不存在或已失效");
    ErrorCode FILE_UPLOAD_CREDENTIAL_EXPIRED = new ErrorCode(1_001_003_021, "上传凭证已过期");
    ErrorCode FILE_UPLOAD_CREDENTIAL_ALREADY_USED = new ErrorCode(1_001_003_022, "上传凭证已使用，不能重复确认");
    ErrorCode FILE_UPLOAD_CREDENTIAL_FORBIDDEN = new ErrorCode(1_001_003_023, "上传凭证不属于当前用户");
    ErrorCode FILE_UPLOAD_TEMP_EMPTY = new ErrorCode(1_001_003_024, "临时对象不存在或为空上传");
    ErrorCode FILE_UPLOAD_TEMP_SIZE_MISMATCH = new ErrorCode(1_001_003_025, "实际上传大小（{}）与凭证声明（{}）不一致");
    ErrorCode FILE_UPLOAD_TEMP_HASH_MISMATCH = new ErrorCode(1_001_003_026, "正式资产散列核验失败，已回滚");
    ErrorCode FILE_PRESIGN_NOT_SUPPORTED = new ErrorCode(1_001_003_027, "当前存储不支持预签名直传，请使用服务端上传");
    // ZS-FILE-004.A：主体绑定交付票据与鉴权取流
    ErrorCode FILE_DELIVERY_TICKET_FORBIDDEN = new ErrorCode(1_001_003_028, "交付票据与当前主体/租户/用途不匹配，拒绝交付");
    ErrorCode FILE_DELIVERY_SESSION_INVALID = new ErrorCode(1_001_003_029, "下载会话不存在或登录会话标识不匹配");
    ErrorCode FILE_DELIVERY_TICKET_REVOKED = new ErrorCode(1_001_003_030, "交付已被撤权（或读权限已回收），拒绝继续交付");
    ErrorCode FILE_DELIVERY_TICKET_EXPIRED = new ErrorCode(1_001_003_031, "交付票据/下载会话已过期");
    // ZS-FILE-005.A：删除中间态与人工对账
    ErrorCode FILE_DELETE_REFERENCED = new ErrorCode(1_001_003_032, "文件存在进行中的交付会话（引用保护），暂不能删除");
    ErrorCode FILE_DELETE_IN_PROGRESS = new ErrorCode(1_001_003_033, "文件删除正在进行中，请勿重复发起");
    ErrorCode FILE_PUBLIC_TYPE_NOT_ALLOWED = new ErrorCode(1_001_003_009, "类型（{}）不在公开素材白名单内，不能转为 PUBLIC");
    ErrorCode FILE_SCOPE_INVALID = new ErrorCode(1_001_003_005, "文件可见范围（{}）非法，仅支持 PUBLIC/PRIVATE"); // codex r0 P3：改用未占用码，原 1_001_003_002 与 FILE_IS_EMPTY 冲突
    ErrorCode FILE_IS_EMPTY = new ErrorCode(1_001_003_002, "文件为空");
    ErrorCode FILE_PATH_INVALID = new ErrorCode(1_001_003_003, "文件路径不正确");

    // ========== 代码生成器 1-001-004-000 ==========
    ErrorCode CODEGEN_TABLE_EXISTS = new ErrorCode(1_001_004_002, "表定义已经存在");
    ErrorCode CODEGEN_IMPORT_TABLE_NULL = new ErrorCode(1_001_004_001, "导入的表不存在");
    ErrorCode CODEGEN_IMPORT_COLUMNS_NULL = new ErrorCode(1_001_004_002, "导入的字段不存在");
    ErrorCode CODEGEN_TABLE_NOT_EXISTS = new ErrorCode(1_001_004_004, "表定义不存在");
    ErrorCode CODEGEN_COLUMN_NOT_EXISTS = new ErrorCode(1_001_004_005, "字段义不存在");
    ErrorCode CODEGEN_SYNC_COLUMNS_NULL = new ErrorCode(1_001_004_006, "同步的字段不存在");
    ErrorCode CODEGEN_SYNC_NONE_CHANGE = new ErrorCode(1_001_004_007, "同步失败，不存在改变");
    ErrorCode CODEGEN_TABLE_INFO_TABLE_COMMENT_IS_NULL = new ErrorCode(1_001_004_008, "数据库的表注释未填写");
    ErrorCode CODEGEN_TABLE_INFO_COLUMN_COMMENT_IS_NULL = new ErrorCode(1_001_004_009, "数据库的表字段({})注释未填写");
    ErrorCode CODEGEN_MASTER_TABLE_NOT_EXISTS = new ErrorCode(1_001_004_010, "主表(id={})定义不存在，请检查");
    ErrorCode CODEGEN_SUB_COLUMN_NOT_EXISTS = new ErrorCode(1_001_004_011, "子表的字段(id={})不存在，请检查");
    ErrorCode CODEGEN_MASTER_GENERATION_FAIL_NO_SUB_TABLE = new ErrorCode(1_001_004_012, "主表生成代码失败，原因：它没有子表");
    ErrorCode CODEGEN_MASTER_TABLE_NAME_DUPLICATE = new ErrorCode(1_001_004_013,
            "主子表规范化类名({})重复，请调整主表或子表类名");
    ErrorCode CODEGEN_MASTER_TABLE_FIELD_DUPLICATE = new ErrorCode(1_001_004_014,
            "主子表属性名({})重复，请调整主子表字段、子表类名或关联关系");

    // ========== 文件配置 1-001-006-000 ==========
    ErrorCode FILE_CONFIG_NOT_EXISTS = new ErrorCode(1_001_006_000, "文件配置不存在");
    ErrorCode FILE_CONFIG_DELETE_FAIL_MASTER = new ErrorCode(1_001_006_001, "该文件配置不允许删除，原因：它是主配置，删除会导致无法上传文件");

    // ========== 数据源配置 1-001-007-000 ==========
    ErrorCode DATA_SOURCE_CONFIG_NOT_EXISTS = new ErrorCode(1_001_007_000, "数据源配置不存在");
    ErrorCode DATA_SOURCE_CONFIG_NOT_OK = new ErrorCode(1_001_007_001, "数据源配置不正确，无法进行连接");

    // ========== 学生 1-001-201-000 ==========
    ErrorCode DEMO01_CONTACT_NOT_EXISTS = new ErrorCode(1_001_201_000, "示例联系人不存在");
    ErrorCode DEMO02_CATEGORY_NOT_EXISTS = new ErrorCode(1_001_201_001, "示例分类不存在");
    ErrorCode DEMO02_CATEGORY_EXITS_CHILDREN = new ErrorCode(1_001_201_002, "存在存在子示例分类，无法删除");
    ErrorCode DEMO02_CATEGORY_PARENT_NOT_EXITS = new ErrorCode(1_001_201_003,"父级示例分类不存在");
    ErrorCode DEMO02_CATEGORY_PARENT_ERROR = new ErrorCode(1_001_201_004, "不能设置自己为父示例分类");
    ErrorCode DEMO02_CATEGORY_NAME_DUPLICATE = new ErrorCode(1_001_201_005, "已经存在该名字的示例分类");
    ErrorCode DEMO02_CATEGORY_PARENT_IS_CHILD = new ErrorCode(1_001_201_006, "不能设置自己的子示例分类为父示例分类");
    ErrorCode DEMO03_STUDENT_NOT_EXISTS = new ErrorCode(1_001_201_007, "学生不存在");
    ErrorCode DEMO03_COURSE_NOT_EXISTS = new ErrorCode(1_001_201_008, "学生课程不存在");
    ErrorCode DEMO03_GRADE_NOT_EXISTS = new ErrorCode(1_001_201_009, "学生班级不存在");
    ErrorCode DEMO03_GRADE_EXISTS = new ErrorCode(1_001_201_010, "学生班级已存在");

}
