import asyncio
import json
from typing import Dict, Any

from core.handle.textMessageHandler import TextMessageHandler
from core.handle.textMessageType import TextMessageType
from core.utils.cache.manager import cache_manager, CacheType
from core.utils.util import get_ip_info
from plugins_func.functions.get_weather import (
    fetch_city_info,
    fetch_now_weather,
    generate_jwt_for_qweather,
)


TAG = __name__


class GetWeatherTextMessageHandler(TextMessageHandler):
    """GetWeatherTextMessageHandler消息处理器"""

    @property
    def message_type(self) -> TextMessageType:
        return TextMessageType.GET_WEATHER

    async def handle(self, conn, msg_json: Dict[str, Any]) -> None:
        location: str | None = msg_json.get("location")
        lang: str = msg_json.get("lang", "zh_CN")
        if lang == "zh_CN":
            lang = "zh"
        conn.logger.bind(tag=TAG).info(
            f"收到GET_WEATHER消息，location={location}, lang={lang}"
        )

        try:
            api_host = conn.config["plugins"]["get_weather"].get("api_host", "nr67cdutyg.re.qweatherapi.com")
            private_key = conn.config["plugins"]["get_weather"].get("private_key","MC4CAQAwBQYDK2VwBCIEIMzD3EyPR67q29b3bytnLkH3pyBq4AIrQcmVbBtAtTBB",)
            kid = conn.config["plugins"]["get_weather"].get("kid", "KFB4P42CMW")
            sub = conn.config["plugins"]["get_weather"].get("sub", "2DKTNQK56X")
            default_location = conn.config["plugins"]["get_weather"]["default_location"]
            client_ip = conn.client_ip

            if not location:
                if client_ip:
                    cached_ip_info = cache_manager.get(CacheType.IP_INFO, client_ip)
                    if cached_ip_info:
                        location = cached_ip_info.get("city")
                    else:
                        ip_info = get_ip_info(client_ip, conn.logger)
                        if ip_info:
                            cache_manager.set(CacheType.IP_INFO, client_ip, ip_info)
                            location = ip_info.get("city")
                if not location:
                    location = default_location

            weather_data = None
            city_id_cache_key = f"weather_city_id_{location}_{lang}"
            city_id = cache_manager.get(CacheType.LOCATION, city_id_cache_key)

            jwt_token = None
            if city_id is None:
                jwt_token = await asyncio.to_thread(
                    generate_jwt_for_qweather, private_key, kid, sub
                )
                city_info = await asyncio.to_thread(
                    fetch_city_info, location, jwt_token, api_host, lang
                )
                if city_info:
                    city_id = city_info.get("id")
                    if city_id:
                        cache_manager.set(
                            CacheType.LOCATION, city_id_cache_key, city_id
                        )

            now_weather_cache_key = f"now_weather_{city_id}_{lang}"
            cached_weather = cache_manager.get(CacheType.WEATHER, now_weather_cache_key)
            if cached_weather is not None:
                weather_data = cached_weather

            if weather_data is None and city_id:
                if jwt_token is None:
                    jwt_token = await asyncio.to_thread(
                        generate_jwt_for_qweather, private_key, kid, sub
                    )
                weather_data = await asyncio.to_thread(
                    fetch_now_weather, city_id, jwt_token, api_host, lang
                )
                if weather_data is not None:
                    cache_manager.set(
                        CacheType.WEATHER, now_weather_cache_key, weather_data
                    )
        except Exception as e:
            conn.logger.bind(tag=TAG).error(f"处理GET_WEATHER消息失败: {e}")
            weather_data = None

        conn.logger.bind(tag=TAG).info(f"最终获取到的天气数据: {weather_data}")
        if weather_data is None:
            weather_data = {"error": "无法获取天气数据，请稍后再试"}
        await conn.websocket.send(
            json.dumps(
                {
                    "type": "get_weather",
                    "session_id": conn.session_id,
                    "data": weather_data,
                }
            )
        )
