-- 直播方案表
CREATE TABLE `live_plan` (
    `id` VARCHAR(32) NOT NULL COMMENT '方案ID',
    `plan_no` VARCHAR(32) NOT NULL COMMENT '方案编号(对外业务号)',
    `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
    `plan_name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '方案名称',
    `platform` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '直播平台',
    `room_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '直播间ID',
    `config_json` JSON COMMENT '方案配置JSON',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0=空闲, 1=使用中',
    `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
    `creator` BIGINT DEFAULT NULL COMMENT '创建者',
    `updater` BIGINT DEFAULT NULL COMMENT '更新者',
    `create_date` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_date` DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_no` (`plan_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_platform` (`platform`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播方案表';
