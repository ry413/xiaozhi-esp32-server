CREATE TABLE `activation_code_batch` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_no` VARCHAR(64) NOT NULL COMMENT '批次号, 建议唯一, 例如 B202604010001',
  `batch_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '批次名称',
  
  `card_type` VARCHAR(20) NOT NULL COMMENT '卡类型: point=点卡, month=月卡',
  `face_value` INT NOT NULL COMMENT '面值: point=秒数, month=天数',
  
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '批次状态: 0=禁用, 1=启用',
  
  `valid_from` DATETIME DEFAULT NULL COMMENT '批次内激活码可使用开始时间, 为空表示不限制',
  `valid_until` DATETIME DEFAULT NULL COMMENT '批次内激活码可使用结束时间, 为空表示永不过期',
  
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
  
  `creator` BIGINT DEFAULT NULL COMMENT '创建者',
  `updater` BIGINT DEFAULT NULL COMMENT '更新者',
  `create_date` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_date` DATETIME DEFAULT NULL COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_no` (`batch_no`),
  KEY `idx_card_type` (`card_type`),
  KEY `idx_status` (`status`),
  KEY `idx_valid_until` (`valid_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='激活码批次表';

CREATE TABLE `activation_code` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_id` BIGINT UNSIGNED NOT NULL COMMENT '批次ID',
  
  `code` VARCHAR(64) NOT NULL COMMENT '激活码, 必须唯一',
  
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0=未使用, 1=已使用, 2=已作废, 3=已过期',
  
  `valid_from` DATETIME DEFAULT NULL COMMENT '此码可使用开始时间, 为空表示不限制',
  `valid_until` DATETIME DEFAULT NULL COMMENT '此码可使用结束时间, 为空表示永不过期',
  
  `used_user_id` BIGINT DEFAULT NULL COMMENT '使用者用户ID',
  `used_at` DATETIME DEFAULT NULL COMMENT '使用时间',
  
  `void_at` DATETIME DEFAULT NULL COMMENT '作废时间',
  `void_reason` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '作废原因',
  
  `creator` BIGINT DEFAULT NULL COMMENT '创建者',
  `updater` BIGINT DEFAULT NULL COMMENT '更新者',
  `create_date` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_date` DATETIME DEFAULT NULL COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_status` (`status`),
  KEY `idx_used_user_id` (`used_user_id`),
  KEY `idx_valid_until` (`valid_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='激活码明细表';

CREATE TABLE `user_balance_account` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `balance_minutes` INT NOT NULL DEFAULT 0 COMMENT '当前剩余秒数',
  `total_recharged_minutes` INT NOT NULL DEFAULT 0 COMMENT '累计充值秒数',
  `total_consumed_minutes` INT NOT NULL DEFAULT 0 COMMENT '累计消费秒数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用, 1=正常',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户点卡余额账户表';

CREATE TABLE `user_balance_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `account_id` BIGINT UNSIGNED NOT NULL COMMENT '余额账户ID',
  `change_type` VARCHAR(20) NOT NULL COMMENT '变更类型: recharge/consume/refund/adjust',
  `delta_minutes` INT NOT NULL COMMENT '变更秒数, 增加为正, 扣减为负',
  `balance_before` INT NOT NULL COMMENT '变更前余额',
  `balance_after` INT NOT NULL COMMENT '变更后余额',
  `source_type` VARCHAR(20) NOT NULL DEFAULT '' COMMENT '来源类型: activation_code/order/admin/task',
  `source_id` BIGINT DEFAULT NULL COMMENT '来源ID',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_source_type_source_id` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户点卡余额流水表';

CREATE TABLE `user_membership` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `membership_type` VARCHAR(20) NOT NULL DEFAULT 'month_card' COMMENT '权益类型, 当前固定 month_card',
  `start_at` DATETIME NOT NULL COMMENT '生效开始时间',
  `end_at` DATETIME NOT NULL COMMENT '生效结束时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=失效, 1=生效中, 2=已回收',
  `source_type` VARCHAR(20) NOT NULL DEFAULT 'activation_code' COMMENT '来源类型: activation_code/admin/manual',
  `source_id` BIGINT DEFAULT NULL COMMENT '来源ID, 例如激活码ID',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_user_status_end_at` (`user_id`, `status`, `end_at`),
  KEY `idx_start_at` (`start_at`),
  KEY `idx_end_at` (`end_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户月卡权益表';

CREATE TABLE `user_membership_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `membership_id` BIGINT UNSIGNED NOT NULL COMMENT '月卡权益ID',
  `change_type` VARCHAR(20) NOT NULL COMMENT '变更类型: create/extend/revoke/expire/adjust',
  `start_at_before` DATETIME DEFAULT NULL COMMENT '变更前开始时间',
  `end_at_before` DATETIME DEFAULT NULL COMMENT '变更前结束时间',
  `start_at_after` DATETIME DEFAULT NULL COMMENT '变更后开始时间',
  `end_at_after` DATETIME DEFAULT NULL COMMENT '变更后结束时间',
  `source_type` VARCHAR(20) NOT NULL DEFAULT '' COMMENT '来源类型: activation_code/admin/manual',
  `source_id` BIGINT DEFAULT NULL COMMENT '来源ID',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_membership_id` (`membership_id`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_source_type_source_id` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户月卡权益流水表';