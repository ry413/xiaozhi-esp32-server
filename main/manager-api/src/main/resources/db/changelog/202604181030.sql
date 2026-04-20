SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_voice_clone'
      AND COLUMN_NAME = 'voice_source_url'
);

SET @sql = IF(
    @col_exists = 0,
    'ALTER TABLE `ai_voice_clone` ADD COLUMN `voice_source_url` VARCHAR(500) DEFAULT NULL COMMENT ''声音样本公网地址'' AFTER `voice`',
    'SELECT ''Column voice_source_url already exists'' AS msg'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
