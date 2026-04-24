CREATE TABLE `user_membership_daily_usage` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `membership_id` BIGINT UNSIGNED NOT NULL COMMENT '月卡权益ID',
  `usage_date` DATE NOT NULL COMMENT '用量日期',
  `daily_limit_seconds` INT NOT NULL DEFAULT 14400 COMMENT '每日限额秒数',
  `consumed_seconds` INT NOT NULL DEFAULT 0 COMMENT '已消费秒数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用, 1=正常',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_usage_date` (`user_id`, `usage_date`),
  KEY `idx_membership_id` (`membership_id`),
  KEY `idx_usage_date` (`usage_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户月卡每日用量表';
