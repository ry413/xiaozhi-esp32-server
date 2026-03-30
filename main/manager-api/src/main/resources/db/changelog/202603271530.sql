-- 添加卖货配置字段到 ai_agent 表（幂等）
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent'
      AND COLUMN_NAME = 'live_id'
);
SET @sql = IF(
    @col_exists = 0,
    'ALTER TABLE `ai_agent` ADD COLUMN `live_id` VARCHAR(64) NULL COMMENT ''直播间ID'' AFTER `sort`',
    'SELECT ''Column live_id already exists'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent'
      AND COLUMN_NAME = 'broadcast_config'
);
SET @sql = IF(
    @col_exists = 0,
    'ALTER TABLE `ai_agent` ADD COLUMN `broadcast_config` JSON NULL COMMENT ''卖货播报配置'' AFTER `live_id`',
    'SELECT ''Column broadcast_config already exists'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent'
      AND COLUMN_NAME = 'awkward_silence_config'
);
SET @sql = IF(
    @col_exists = 0,
    'ALTER TABLE `ai_agent` ADD COLUMN `awkward_silence_config` JSON NULL COMMENT ''冷场话术配置'' AFTER `broadcast_config`',
    'SELECT ''Column awkward_silence_config already exists'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent'
      AND COLUMN_NAME = 'prompt_flow_config'
);
SET @sql = IF(
    @col_exists = 0,
    'ALTER TABLE `ai_agent` ADD COLUMN `prompt_flow_config` JSON NULL COMMENT ''提示词流程配置'' AFTER `awkward_silence_config`',
    'SELECT ''Column prompt_flow_config already exists'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
