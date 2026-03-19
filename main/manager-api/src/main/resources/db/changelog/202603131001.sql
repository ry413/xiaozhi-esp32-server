START TRANSACTION;

UPDATE `ai_model_provider`
SET `fields` = JSON_ARRAY(
        JSON_OBJECT(
                'key', 'default_location',
                'type', 'string',
                'label', '默认查询城市',
                'default', '广州',
                'selected', false
        ),
        JSON_OBJECT(
                'key', 'api_host',
                'type', 'string',
                'label', '开发者 API Host',
                'default', 'nr67cdutyg.re.qweatherapi.com',
                'editing', false,
                'selected', false
        ),
        JSON_OBJECT(
                'key', 'private_key',
                'type', 'string',
                'label', '私钥内容',
                'default', '',
                'editing', false,
                'selected', false
        ),
        JSON_OBJECT(
                'key', 'kid',
                'type', 'string',
                'label', '凭据ID',
                'default', '',
                'editing', false,
                'selected', false
        ),
        JSON_OBJECT(
                'key', 'sub',
                'type', 'string',
                'label', '项目ID',
                'default', '',
                'editing', false,
                'selected', false
        )
    )
WHERE `provider_code` = 'get_weather'
  AND `model_type` = 'Plugin';

COMMIT;
