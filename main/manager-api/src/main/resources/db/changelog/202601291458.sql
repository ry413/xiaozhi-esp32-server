-- 给 sys_user 表新增 hidden_builtin_wallpaper_ids 字段
ALTER TABLE `sys_user`
ADD COLUMN `hidden_builtin_wallpaper_ids` json DEFAULT NULL COMMENT '隐藏内置壁纸id集' AFTER `update_date`;
