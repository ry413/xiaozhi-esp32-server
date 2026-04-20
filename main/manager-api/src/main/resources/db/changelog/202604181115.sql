delete from `ai_model_provider` where id = 'SYSTEM_TTS_CosyVoiceCloneStream';
INSERT INTO `ai_model_provider` (`id`, `model_type`, `provider_code`, `name`, `fields`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('SYSTEM_TTS_CosyVoiceCloneStream', 'TTS', 'cosyvoice_clone_stream', 'CosyVoice克隆语音合成', '[{"key":"api_key","label":"API密钥","type":"string"},{"key":"output_dir","label":"输出目录","type":"string"},{"key":"model","label":"模型","type":"string"},{"key":"voice","type":"string","label":"音色"},{"key":"format","label":"音频格式","type":"string"},{"key":"volume","label":"音量","type":"number"},{"key":"rate","type":"number","label":"语速"},{"key":"pitch","type":"number","label":"音调"},{"key":"prefix","type":"string","label":"音频资源id前缀"}]', 21, 1, NOW(), 1, NOW());

delete from `ai_model_config` where id = 'TTS_CosyVoiceStreamTTS';
INSERT INTO `ai_model_config` VALUES ('TTS_CosyVoiceStreamTTS', 'tts', 'cosyvoice_clone_stream', 'CosyVoice克隆语音合成(流式)', 0, 1, '{"api_key":"","type":"cosyvoice_clone_stream","output_dir":"/tmp","model":"cosyvoice-v3.5-flash","voice":"null","format":"wav","volume":"50","rate":"1","pitch":"1","prefix":"xzclone"}', '', '', 1, NULL, NULL, NULL, NULL);
