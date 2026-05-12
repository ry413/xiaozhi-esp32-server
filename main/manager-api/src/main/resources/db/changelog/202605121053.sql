SET @col_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_model_config'
    AND COLUMN_NAME = 'benefit_consume_multiplier'
);
SET @sql = IF(
  @col_exists = 0,
  'ALTER TABLE `ai_model_config` ADD COLUMN `benefit_consume_multiplier` DECIMAL(8,3) NOT NULL DEFAULT 1.000 COMMENT ''权益消费倍率，仅TTS模型生效'' AFTER `is_enabled`',
  'SELECT ''Column benefit_consume_multiplier already exists'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `ai_model_config`
SET `benefit_consume_multiplier` = 1.000
WHERE LOWER(`model_type`) = 'tts'
  AND (`benefit_consume_multiplier` IS NULL OR `benefit_consume_multiplier` <= 0);