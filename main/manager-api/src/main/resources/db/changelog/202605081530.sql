
INSERT INTO ai_model_provider (id, model_type, provider_code, name, fields,
                               sort, creator, create_date, updater, update_date)
VALUES ('SYSTEM_PLUGIN_HANDLE_EXIT_INTENT',
        'Plugin',
        'handle_exit_intent',
        '识别退出意图',
        JSON_ARRAY(),
        9, 0, NOW(), 0, NOW());