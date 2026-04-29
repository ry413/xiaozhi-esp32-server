delete from `ai_model_provider` where id = 'SYSTEM_TTS_QwenTTSStream';
INSERT INTO `ai_model_provider` (`id`, `model_type`, `provider_code`, `name`, `fields`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('SYSTEM_TTS_QwenTTSStream', 'TTS', 'qwen_tts_stream', 'QwenTTS流式语音合成', '[{"key":"api_key","label":"API密钥","type":"string"},{"key":"output_dir","label":"输出目录","type":"string"},{"key":"model","label":"模型","type":"string"},{"key":"voice","label":"音色","type":"string"},{"key":"format","label":"音频格式","type":"string"}]', 21, 1, NOW(), 1, NOW());

delete from `ai_model_config` where id = 'TTS_QwenTTSStream';
INSERT INTO `ai_model_config` VALUES ('TTS_QwenTTSStream', 'TTS', 'QwenTTSStream', 'QwenTTS流式语音合成', 0, 1, '{"type":"qwen_tts_stream","api_key":"","output_dir":"tmp/","model":"qwen-tts-latest","voice":"Cherry","format":"pcm"}', NULL, NULL, 22, NULL, NULL, NULL, NULL);
