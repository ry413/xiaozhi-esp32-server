-- 微信用户映射表
DROP TABLE IF EXISTS sys_user_wechat;
CREATE TABLE sys_user_wechat (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  appid         VARCHAR(64) NOT NULL,
  openid        VARCHAR(128) NOT NULL,
  unionid       VARCHAR(128) NULL,
  create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login    DATETIME NULL,

  UNIQUE KEY uk_appid_openid (appid, openid),
  UNIQUE KEY uk_unionid (unionid),
  KEY idx_user_id (user_id)
);
