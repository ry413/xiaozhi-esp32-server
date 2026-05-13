import io
import wave

from config.logger import setup_logging
from core.providers.tts.base import TTSProviderBase

TAG = __name__
logger = setup_logging()


class TTSProvider(TTSProviderBase):
    def __init__(self, config, delete_audio_file):
        super().__init__(config, delete_audio_file)
        self.audio_file_type = str(config.get("format", "wav")).lower()
        self.sample_rate = int(config.get("sample_rate", 24000))
        self.channels = int(config.get("channels", 1))
        self.sample_width = 2
        self.duration_ms = int(config.get("duration_ms", 1000))

        if self.channels != 1:
            raise ValueError("mock TTS 目前仅支持单声道 channels=1")
        if self.audio_file_type not in ("wav", "pcm"):
            raise ValueError("mock TTS 目前仅支持 format=wav 或 format=pcm")

    def _build_silence_bytes(self) -> bytes:
        sample_rate = self.conn.sample_rate if self.conn else self.sample_rate
        frame_count = max(1, int(sample_rate * self.duration_ms / 1000))
        pcm_data = b"\x00" * frame_count * self.channels * self.sample_width

        if self.audio_file_type == "pcm":
            return pcm_data

        buffer = io.BytesIO()
        with wave.open(buffer, "wb") as wav_file:
            wav_file.setnchannels(self.channels)
            wav_file.setsampwidth(self.sample_width)
            wav_file.setframerate(sample_rate)
            wav_file.writeframes(pcm_data)
        return buffer.getvalue()

    async def text_to_speak(self, text, output_file):
        audio_bytes = self._build_silence_bytes()
        logger.bind(tag=TAG).debug(
            f"mock TTS 返回静音音频: format={self.audio_file_type}, duration_ms={self.duration_ms}, text_len={len(text) if text else 0}"
        )

        if output_file:
            with open(output_file, "wb") as file:
                file.write(audio_bytes)
            return None

        return audio_bytes
