SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_voice_clone'
      AND COLUMN_NAME = 'voice_id'
);

SET @sql = IF(
    @col_exists = 1,
    'ALTER TABLE `ai_voice_clone` MODIFY COLUMN `voice_id` VARCHAR(128) COMMENT ''声音id''',
    'SELECT ''Column voice_id does not exist'' AS msg'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
