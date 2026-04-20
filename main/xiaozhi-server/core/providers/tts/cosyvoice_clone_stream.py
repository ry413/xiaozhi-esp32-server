import dashscope

from config.logger import setup_logging
from core.providers.tts.base import TTSProviderBase
from dashscope.audio.tts_v2 import AudioFormat, SpeechSynthesizer

TAG = __name__
logger = setup_logging()


class TTSProvider(TTSProviderBase):
    TTS_PARAM_CONFIG = [
        ("ttsVolume", "volume", 0, 100, 50, int),
        ("ttsRate", "rate", 0.5, 2.0, 1.0, lambda v: round(v, 1)),
        ("ttsPitch", "pitch", 0.5, 2.0, 1.0, lambda v: round(v, 1)),
    ]

    def __init__(self, config, delete_audio_file):
        super().__init__(config, delete_audio_file)
        self.api_key = config.get("api_key")
        if not self.api_key:
            raise ValueError("api_key is required for CosyVoice clone TTS")

        self.model = config.get("model", "cosyvoice-v3.5-flash")
        self.voice = config.get("private_voice") or config.get("voice")
        if not self.voice:
            raise ValueError("voice/private_voice is required for CosyVoice clone TTS")

        self.ws_url = "wss://dashscope.aliyuncs.com/api-ws/v1/inference"

        format_name = str(config.get("format", "wav")).lower()
        self.audio_file_type = format_name
        self.output_format = self._resolve_audio_format(format_name)

        volume = config.get("volume", "50")
        self.volume = int(volume) if volume else 50

        rate = config.get("rate", "1.0")
        self.rate = float(rate) if rate else 1.0

        pitch = config.get("pitch", "1.0")
        self.pitch = float(pitch) if pitch else 1.0

        self._apply_percentage_params(config)

    def _resolve_audio_format(self, format_name):
        sample_rate = 24000
        if format_name == "pcm":
            return AudioFormat.PCM_24000HZ_MONO_16BIT
        if format_name == "mp3":
            return AudioFormat.MP3_24000HZ_MONO_256KBPS
        return AudioFormat.WAV_24000HZ_MONO_16BIT

    async def text_to_speak(self, text, output_file):
        dashscope.api_key = self.api_key
        dashscope.base_websocket_api_url = self.ws_url

        synthesizer = SpeechSynthesizer(
            model=self.model,
            voice=self.voice,
            format=self.output_format,
            volume=self.volume,
            speech_rate=self.rate,
            pitch_rate=self.pitch,
        )

        audio_data = synthesizer.call(text, timeout_millis=120000)
        if not audio_data:
            response = None
            try:
                response = synthesizer.get_response()
            except Exception:
                pass
            raise RuntimeError(f"CosyVoice TTS未返回有效音频数据, response={response}")

        logger.bind(tag=TAG).info(
            f"CosyVoice TTS获取音频成功, bytes={len(audio_data)}, model={self.model}, voice={self.voice}, format={self.audio_file_type}"
        )

        if output_file:
            with open(output_file, "wb") as file_to_save:
                file_to_save.write(audio_data)
            return None

        return audio_data

