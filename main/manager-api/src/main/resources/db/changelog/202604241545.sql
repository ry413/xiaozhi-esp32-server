CREATE TABLE `ai_agent_llm_report` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `mac_address` VARCHAR(64) NOT NULL COMMENT 'MAC地址/设备ID',
  `client_id` VARCHAR(128) DEFAULT NULL COMMENT '客户端ID',
  `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
  `agent_id` VARCHAR(32) DEFAULT NULL COMMENT '智能体ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '设备所属用户ID',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `llm_input` LONGTEXT NOT NULL COMMENT '大模型输入',
  `llm_output` LONGTEXT NOT NULL COMMENT '大模型输出',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间/上报时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_created_at` (`user_id`, `created_at`),
  KEY `idx_mac_created_at` (`mac_address`, `created_at`),
  KEY `idx_agent_created_at` (`agent_id`, `created_at`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体大模型调用记录表';
