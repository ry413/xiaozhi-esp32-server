ALTER TABLE ai_device
  ADD COLUMN auto_start_plan_no VARCHAR(32) NULL COMMENT '设备开机时自动启动导播方案的id' AFTER wallpaper_ids;
