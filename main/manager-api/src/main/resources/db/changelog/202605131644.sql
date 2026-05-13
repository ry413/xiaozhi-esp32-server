delete from `ai_model_provider` where id = 'SYSTEM_TTS_MockTTS';
INSERT INTO `ai_model_provider` (`id`, `model_type`, `provider_code`, `name`, `fields`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('SYSTEM_TTS_MockTTS', 'TTS', 'mock_tts', 'MockTTS语音合成', '[{"key":"output_dir","label":"输出目录","type":"string"},{"key":"format","label":"音频格式","type":"string"},{"key":"duration_ms","label":"音频时长（毫秒）","type":"number"},{"key":"sample_rate","label":"采样率","type":"number"}]', 42, 1, NOW(), 1, NOW());

delete from `ai_model_config` where id = 'TTS_MockTTS';
INSERT INTO `ai_model_config`
(`id`, `model_type`, `model_code`, `model_name`, `is_default`, `is_enabled`, `benefit_consume_multiplier`, `config_json`, `doc_link`, `remark`, `sort`, `creator`, `create_date`, `updater`, `update_date`)
VALUES
('TTS_MockTTS', 'TTS', 'MockTTS', 'MockTTS语音合成', 0, 1, 1.000, '{"type":"mock_tts","output_dir":"tmp/","format":"wav","duration_ms":5000,"sample_rate":24000}', NULL, NULL, 42, NULL, NULL, NULL, NULL);
