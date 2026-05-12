ALTER TABLE sys_user_wechat
  ADD COLUMN nickname VARCHAR(128) NULL COMMENT '微信昵称' AFTER unionid;
