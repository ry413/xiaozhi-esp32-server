-- 设备壁纸表
DROP TABLE IF EXISTS `sys_wallpapers`;
CREATE TABLE `sys_wallpapers` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `is_builtin` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置壁纸',
  `url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '壁纸URL',
  `create_date` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='壁纸表';

-- 给 ai_device 表新增 wallpaper_ids 字段
ALTER TABLE `ai_device`
ADD COLUMN `wallpaper_ids` json DEFAULT NULL COMMENT '壁纸id集' AFTER `update_date`;
