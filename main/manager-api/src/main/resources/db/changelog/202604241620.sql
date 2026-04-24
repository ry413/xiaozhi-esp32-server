ALTER TABLE `ai_agent`
    ADD COLUMN `prompt_template` varchar(255) DEFAULT NULL COMMENT '提示词模板文件' AFTER `system_prompt`;

ALTER TABLE `ai_agent_template`
    ADD COLUMN `prompt_template` varchar(255) DEFAULT NULL COMMENT '提示词模板文件' AFTER `system_prompt`;
