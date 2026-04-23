import os
import time
import queue
import aiohttp
import asyncio
import requests
import traceback

from config.logger import setup_logging
from core.utils.tts import MarkdownCleaner
from core.providers.tts.base import TTSProviderBase
from core.utils import opus_encoder_utils, textUtils
from core.providers.tts.dto.dto import SentenceType, ContentType, InterfaceType

TAG = __name__
logger = setup_logging()


class TTSProvider(TTSProviderBase):
    TTS_PARAM_CONFIG = [
        # ("ttsVolume", "volume", 0, 100, 50, int),
        ("ttsRate", "rate", 0.5, 2.0, 1.0, lambda v: round(v, 1)),
        # ("ttsPitch", "pitch", 0.5, 2.0, 1.0, lambda v: round(v, 1)),
    ]
    def __init__(self, config, delete_audio_file):
        super().__init__(config, delete_audio_file)
        self.interface_type = InterfaceType.SINGLE_STREAM

        self.base_url = config.get("base_url", "http://127.0.0.1:17890")
        self.endpoint = config.get("endpoint", "/tts/stream")
        self.api_url = config.get("api_url") or (
            f"{self.base_url.rstrip('/')}{self.endpoint}"
        )

        self.voice = config.get("private_voice") or config.get("voice_id")
        if not self.voice:
            raise ValueError("voice_id/private_voice is required for cosyvoice_local_stream")

        rate = config.get("rate", "1.0")
        self.rate = float(rate) if rate else 1.0
        self.audio_format = "pcm"
        self.expected_audio_format = config.get("format", "pcm_s16le")
        self.expected_channels = int(config.get("channels", 1))
        self.request_sample_rate = config.get("sample_rate")
        self.before_stop_play_files = []
        self.pcm_buffer = bytearray()

        self._apply_percentage_params(config)

    async def open_audio_channels(self, conn):
        await super().open_audio_channels(conn)

    def tts_text_priority_thread(self):
        while not self.conn.stop_event.is_set():
            try:
                message = self.tts_text_queue.get(timeout=1)
                if message.sentence_type == SentenceType.FIRST:
                    self.tts_stop_request = False
                    self.processed_chars = 0
                    self.tts_text_buff = []
                    self.before_stop_play_files.clear()
                elif ContentType.TEXT == message.content_type:
                    self.tts_text_buff.append(message.content_detail)
                    segment_text = self._get_segment_text()
                    if segment_text:
                        self.to_tts_single_stream(segment_text)
                elif ContentType.FILE == message.content_type:
                    logger.bind(tag=TAG).info(
                        f"添加音频文件到待播放列表: {message.content_file}"
                    )
                    if message.content_file and os.path.exists(message.content_file):
                        self._process_audio_file_stream(
                            message.content_file,
                            callback=lambda audio_data: self.handle_audio_file(
                                audio_data, message.content_detail
                            ),
                        )

                if message.sentence_type == SentenceType.LAST:
                    self._process_remaining_text_stream(True)

            except queue.Empty:
                continue
            except Exception as e:
                logger.bind(tag=TAG).error(
                    f"处理TTS文本失败: {str(e)}, 类型: {type(e).__name__}, 堆栈: {traceback.format_exc()}"
                )

    def _process_remaining_text_stream(self, is_last=False):
        full_text = "".join(self.tts_text_buff)
        remaining_text = full_text[self.processed_chars :]
        if remaining_text:
            segment_text = textUtils.get_string_no_punctuation_or_emoji(remaining_text)
            if segment_text:
                self.to_tts_single_stream(segment_text, is_last)
                self.processed_chars += len(full_text)
            else:
                self._process_before_stop_play_files()
        else:
            self._process_before_stop_play_files()

    def to_tts_single_stream(self, text, is_last=False):
        try:
            max_repeat_time = 5
            text = MarkdownCleaner.clean_markdown(text)
            while max_repeat_time > 0:
                try:
                    asyncio.run(self.text_to_speak(text, is_last))
                    break
                except Exception as e:
                    logger.bind(tag=TAG).warning(
                        f"语音生成失败{6 - max_repeat_time}次: {text}，错误: {e}"
                    )
                    max_repeat_time -= 1

            if max_repeat_time > 0:
                logger.bind(tag=TAG).info(
                    f"语音生成成功: {text}，重试{5 - max_repeat_time}次"
                )
            else:
                logger.bind(tag=TAG).error(
                    f"语音生成失败: {text}，请检查本地TTS服务是否正常"
                )
        except Exception as e:
            logger.bind(tag=TAG).error(f"Failed to stream TTS audio: {e}")
        finally:
            return None

    def _build_request_data(self, text):
        data = {
            "voice_id": self.voice,
            "text": text,
            # 流式接口不支持speed参数, 通过instruct_text提示用户调整语速
            "instruct_text": (
                "请尽可能快地说这句话"
                if self.rate > 1.0
                else "请尽可能慢地说这句话"
                if self.rate < 1.0
                else ""
            )
        }
        if self.request_sample_rate:
            data["sample_rate"] = str(self.request_sample_rate)
        return data

    def _validate_response_headers(self, headers):
        sample_rate = int(headers.get("X-Sample-Rate", self.conn.sample_rate))
        channels = int(headers.get("X-Channels", self.expected_channels))
        audio_format = headers.get("X-Audio-Format", self.expected_audio_format)

        if audio_format != self.expected_audio_format:
            raise ValueError(
                f"unsupported audio format: {audio_format}, expected {self.expected_audio_format}"
            )
        if channels != self.expected_channels:
            raise ValueError(
                f"unsupported channels: {channels}, expected {self.expected_channels}"
            )
        if sample_rate != self.conn.sample_rate:
            raise ValueError(
                f"sample rate mismatch: response={sample_rate}, conn={self.conn.sample_rate}"
            )

        return sample_rate

    async def text_to_speak(self, text, is_last):
        timeout = aiohttp.ClientTimeout(total=None, connect=10, sock_read=300)
        data = self._build_request_data(text)

        try:
            async with aiohttp.ClientSession(timeout=timeout) as session:
                async with session.post(self.api_url, data=data) as resp:
                    if resp.status != 200:
                        raise RuntimeError(
                            f"TTS请求失败: {resp.status}, {await resp.text()}"
                        )

                    sample_rate = self._validate_response_headers(resp.headers)
                    frame_bytes = int(sample_rate * 1 * 60 / 1000 * 2)

                    self.pcm_buffer.clear()
                    self.tts_audio_queue.put((SentenceType.FIRST, [], text))

                    async for chunk in resp.content.iter_any():
                        data_chunk = chunk[0] if isinstance(chunk, (list, tuple)) else chunk
                        if not data_chunk:
                            continue

                        self.pcm_buffer.extend(data_chunk)
                        while len(self.pcm_buffer) >= frame_bytes:
                            frame = bytes(self.pcm_buffer[:frame_bytes])
                            del self.pcm_buffer[:frame_bytes]
                            self.opus_encoder.encode_pcm_to_opus_stream(
                                frame,
                                end_of_stream=False,
                                callback=self.handle_opus,
                            )

                    if self.pcm_buffer:
                        self.opus_encoder.encode_pcm_to_opus_stream(
                            bytes(self.pcm_buffer),
                            end_of_stream=True,
                            callback=self.handle_opus,
                        )
                        self.pcm_buffer.clear()

                    if is_last:
                        self._process_before_stop_play_files()

        except Exception as e:
            logger.bind(tag=TAG).error(f"TTS请求异常: {e}")
            self.tts_audio_queue.put((SentenceType.LAST, [], None))
            raise

    def to_tts(self, text: str) -> list:
        start_time = time.time()
        text = MarkdownCleaner.clean_markdown(text)
        data = self._build_request_data(text)

        try:
            with requests.post(
                self.api_url,
                data=data,
                stream=True,
                timeout=(10, 300),
            ) as response:
                if response.status_code != 200:
                    logger.bind(tag=TAG).error(
                        f"TTS请求失败: {response.status_code}, {response.text}"
                    )
                    return []

                sample_rate = self._validate_response_headers(response.headers)
                logger.bind(tag=TAG).info(
                    f"TTS请求成功: {text}, 耗时: {time.time() - start_time}秒, sample_rate={sample_rate}"
                )

                opus_datas = []
                pcm_buffer = bytearray()
                frame_bytes = int(sample_rate * 1 * 60 / 1000 * 2)

                for chunk in response.iter_content(chunk_size=4096):
                    if not chunk:
                        continue

                    pcm_buffer.extend(chunk)
                    while len(pcm_buffer) >= frame_bytes:
                        frame = bytes(pcm_buffer[:frame_bytes])
                        del pcm_buffer[:frame_bytes]
                        self.opus_encoder.encode_pcm_to_opus_stream(
                            frame,
                            end_of_stream=False,
                            callback=lambda opus: opus_datas.append(opus),
                        )

                if pcm_buffer:
                    self.opus_encoder.encode_pcm_to_opus_stream(
                        bytes(pcm_buffer),
                        end_of_stream=True,
                        callback=lambda opus: opus_datas.append(opus),
                    )

                return opus_datas

        except Exception as e:
            logger.bind(tag=TAG).error(f"TTS请求异常: {e}")
            return []

    async def close(self):
        await super().close()
        if hasattr(self, "opus_encoder"):
            self.opus_encoder.close()

