import base64
import json
import os
import queue
import time
import traceback

import asyncio
import dashscope

from config.logger import setup_logging
from core.providers.tts.base import TTSProviderBase
from core.providers.tts.dto.dto import SentenceType, ContentType, InterfaceType
from core.utils import opus_encoder_utils, textUtils
from core.utils.tts import MarkdownCleaner

TAG = __name__
logger = setup_logging()


class TTSProvider(TTSProviderBase):
    TTS_PARAM_CONFIG = [
        ("ttsVolume", "volume", 0, 100, 50, int),
        ("ttsRate", "rate", 0.5, 2.0, 1.0, lambda v: round(v, 1)),
    ]

    def __init__(self, config, delete_audio_file):
        super().__init__(config, delete_audio_file)
        self.interface_type = InterfaceType.SINGLE_STREAM
        self.api_key = config.get("api_key")
        if not self.api_key:
            raise ValueError("api_key is required for Qwen TTS")

        self.model = config.get("model", "qwen3-tts-instruct-flash")
        self.voice = config.get("private_voice") or config.get("voice", "Cherry")
        self.language_type = config.get(
            "language_type", config.get("language", "Chinese")
        )
        self.base_http_api_url = config.get(
            "base_http_api_url",
            getattr(dashscope, "base_http_api_url", None)
            or "https://dashscope.aliyuncs.com/api/v1",
        )

        self.sample_rate = int(config.get("sample_rate", 24000))
        self.format = config.get("format", "pcm")
        self.audio_file_type = "pcm"
        self.output_file = config.get("output_dir", "tmp/")
        self.report_on_last = True
        self.before_stop_play_files = []

        self.volume = int(config.get("volume", 50) or 50)
        self.rate = float(config.get("rate", 1.0) or 1.0)
        self._apply_percentage_params(config)

        self.instructions = self._build_rate_instruction()
        if self.instructions and "instruct" not in self.model:
            logger.bind(tag=TAG).warning(
                f"Qwen TTS模型{self.model}不支持instructions，已忽略rate语速指令"
            )
            self.instructions = None

        self.opus_encoder = opus_encoder_utils.OpusEncoderUtils(
            sample_rate=self.sample_rate, channels=1, frame_size_ms=60
        )
        self.pcm_buffer = bytearray()

        logger.bind(tag=TAG).info(
            f"Inmbns={self.instructions}"
        )

    def tts_text_priority_thread(self):
        while not self.conn.stop_event.is_set():
            try:
                message = self.tts_text_queue.get(timeout=1)

                if self.conn.client_abort:
                    logger.bind(tag=TAG).info("收到打断信息，终止Qwen TTS文本处理线程")
                    continue

                if message.sentence_id != self.conn.sentence_id:
                    continue

                if message.sentence_type == SentenceType.FIRST:
                    self.current_sentence_id = message.sentence_id
                    self.tts_stop_request = False
                    self.processed_chars = 0
                    self.tts_text_buff = []
                    self.is_first_sentence = True
                    self.tts_audio_first_sentence = True
                    self.before_stop_play_files.clear()

                elif ContentType.TEXT == message.content_type:
                    self.tts_text_buff.append(message.content_detail)
                    segment_text = self._get_segment_text()
                    if segment_text:
                        self.to_tts_single_stream(segment_text)

                elif ContentType.FILE == message.content_type:
                    if message.content_file and os.path.exists(message.content_file):
                        self._process_audio_file_stream(
                            message.content_file,
                            callback=lambda audio_data: self.handle_audio_file(
                                audio_data, message.content_detail
                            ),
                        )

                if message.sentence_type == SentenceType.LAST:
                    self._process_remaining_text_stream(is_last=True)

            except queue.Empty:
                continue
            except Exception as e:
                logger.bind(tag=TAG).error(
                    f"处理Qwen TTS文本失败: {str(e)}, 类型: {type(e).__name__}, 堆栈: {traceback.format_exc()}"
                )

    def _process_remaining_text_stream(self, is_last=False):
        full_text = "".join(self.tts_text_buff)
        remaining_text = full_text[self.processed_chars :]
        if remaining_text:
            segment_text = textUtils.get_string_no_punctuation_or_emoji(remaining_text)
            if segment_text:
                self.to_tts_single_stream(segment_text, is_last=is_last)
                self.processed_chars = len(full_text)
                return

        if is_last:
            self._process_before_stop_play_files()

    def to_tts_single_stream(self, text, is_last=False):
        text = MarkdownCleaner.clean_markdown(text)
        if not text:
            if is_last:
                self._process_before_stop_play_files()
            return

        max_repeat_time = 5
        for attempt in range(1, max_repeat_time + 1):
            try:
                asyncio.run(self.text_to_speak(text, is_last=is_last))
                logger.bind(tag=TAG).info(f"Qwen TTS语音生成成功: {text}，重试{attempt - 1}次")
                return
            except Exception as e:
                logger.bind(tag=TAG).warning(
                    f"Qwen TTS语音生成失败{attempt}次: {text}，错误: {e}"
                )

        logger.bind(tag=TAG).error(f"Qwen TTS语音生成失败: {text}，请检查网络或服务是否正常")
        if is_last:
            self.tts_audio_queue.put(
                (SentenceType.LAST, [], None, getattr(self, "current_sentence_id", None))
            )

    async def text_to_speak(self, text, output_file=None, is_last=False):
        audio_chunks = []
        audio_bytes_count = 0
        opus_packet_count = 0
        self.pcm_buffer.clear()
        self.tts_audio_queue.put(
            (SentenceType.FIRST, [], text, getattr(self, "current_sentence_id", None))
        )

        frame_bytes = int(self.sample_rate * 1 * 60 / 1000 * 2)
        for audio_bytes in self._iter_qwen_tts_audio(text):
            if self.conn and self.conn.client_abort:
                break

            audio_bytes_count += len(audio_bytes)
            if output_file:
                audio_chunks.append(audio_bytes)
            else:
                opus_packet_count += self._write_pcm_to_opus(
                    audio_bytes, frame_bytes, end_of_stream=False
                )

        if audio_bytes_count == 0 and self._has_qwen_tts_api():
            logger.bind(tag=TAG).warning("Qwen TTS入口未解析到音频，切换到tts回调接口重试")
            for audio_bytes in self._iter_legacy_tts_audio(text):
                if self.conn and self.conn.client_abort:
                    break

                audio_bytes_count += len(audio_bytes)
                if output_file:
                    audio_chunks.append(audio_bytes)
                else:
                    opus_packet_count += self._write_pcm_to_opus(
                        audio_bytes, frame_bytes, end_of_stream=False
                    )

        if output_file:
            with open(output_file, "wb") as file_to_save:
                file_to_save.write(b"".join(audio_chunks))
            return None

        if self.pcm_buffer:
            opus_packet_count += self._write_pcm_to_opus(
                b"", frame_bytes, end_of_stream=True
            )

        logger.bind(tag=TAG).info(
            f"Qwen TTS流式音频接收完成: text={text}, audio_bytes={audio_bytes_count}, opus_packets={opus_packet_count}"
        )

        if is_last:
            time.sleep(0.05)
            self._process_before_stop_play_files()

        return b"".join(audio_chunks) if audio_chunks else None

    def _write_pcm_to_opus(self, audio_bytes, frame_bytes, end_of_stream=False):
        opus_packet_count = 0
        if audio_bytes:
            self.pcm_buffer.extend(audio_bytes)

        while len(self.pcm_buffer) >= frame_bytes:
            frame = bytes(self.pcm_buffer[:frame_bytes])
            del self.pcm_buffer[:frame_bytes]
            self.opus_encoder.encode_pcm_to_opus_stream(
                frame,
                end_of_stream=False,
                callback=lambda opus: self._handle_opus_with_count(opus),
            )
            opus_packet_count += 1

        if end_of_stream and self.pcm_buffer:
            self.opus_encoder.encode_pcm_to_opus_stream(
                bytes(self.pcm_buffer),
                end_of_stream=True,
                callback=lambda opus: self._handle_opus_with_count(opus),
            )
            opus_packet_count += 1
            self.pcm_buffer.clear()

        return opus_packet_count

    def _handle_opus_with_count(self, opus):
        if opus:
            self.handle_opus(opus)

    def _call_qwen_tts(self, text):
        if hasattr(dashscope.audio, "qwen_tts"):
            kwargs = {
                "model": self.model,
                "api_key": self.api_key,
                "text": text,
                "voice": self.voice,
                "stream": True,
            }
            if self.instructions:
                kwargs["instructions"] = self.instructions
            return dashscope.audio.qwen_tts.SpeechSynthesizer.call(**kwargs)

        if self._supports_multimodal_text_call():
            kwargs = {
                "model": self.model,
                "api_key": self.api_key,
                "text": text,
                "voice": self.voice,
                "language_type": self.language_type,
                "stream": True,
            }
            if self.instructions:
                kwargs["instructions"] = self.instructions
                kwargs["optimize_instructions"] = True
            return dashscope.MultiModalConversation.call(**kwargs)

        raise RuntimeError("当前dashscope SDK没有Qwen TTS可用入口")

    def _has_qwen_tts_api(self):
        return hasattr(dashscope.audio, "qwen_tts") or hasattr(
            dashscope, "MultiModalConversation"
        )

    def _supports_multimodal_text_call(self):
        if not hasattr(dashscope, "MultiModalConversation"):
            return False

        try:
            import inspect

            params = inspect.signature(
                dashscope.MultiModalConversation.call
            ).parameters
            if "text" in params:
                return True
            messages = params.get("messages")
            return messages is None or messages.default is not inspect._empty
        except Exception:
            return True

    def _build_rate_instruction(self):
        if self.rate > 1:
            return "请用明显偏快的语速朗读，节奏紧凑，停顿尽量短，表达自然清晰。"
        if self.rate < 1:
            return "请用明显偏慢的语速朗读，节奏舒缓，停顿稍长，表达自然清晰。"
        return None

    def _iter_qwen_tts_audio(self, text):
        if (
            hasattr(dashscope.audio, "qwen_tts")
            or self._supports_multimodal_text_call()
        ):
            response = self._call_qwen_tts(text)
            if not hasattr(response, "__iter__") or isinstance(
                response, (dict, bytes, bytearray, str)
            ):
                response = [response]

            for chunk in response:
                audio_data = self._get_audio_data(chunk)
                if audio_data:
                    yield self._decode_audio_data(audio_data)
            return

        if hasattr(dashscope, "MultiModalConversation"):
            yield from self._iter_http_qwen_tts_audio(text)
            return

        yield from self._iter_legacy_tts_audio(text)

    def _iter_http_qwen_tts_audio(self, text):
        import requests

        url = (
            self.base_http_api_url.rstrip("/")
            + "/services/aigc/multimodal-generation/generation"
        )
        input_data = {
            "text": text,
            "voice": self.voice,
            "language_type": self.language_type,
        }
        payload = {
            "model": self.model,
            "input": input_data,
            "parameters": {},
        }
        if self.instructions:
            payload["parameters"]["instructions"] = self.instructions
            payload["parameters"]["optimize_instructions"] = True
        if not payload["parameters"]:
            payload.pop("parameters")

        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
            "X-DashScope-SSE": "enable",
        }

        with requests.post(
            url, headers=headers, json=payload, stream=True, timeout=60
        ) as response:
            response.raise_for_status()
            for raw_line in response.iter_lines(decode_unicode=True):
                if not raw_line or not raw_line.startswith("data:"):
                    continue

                data = raw_line[len("data:") :].strip()
                if data == "[DONE]":
                    break

                chunk = json.loads(data)
                status_code = chunk.get("status_code")
                if status_code and status_code != 200:
                    raise RuntimeError(f"Qwen TTS HTTP错误: {data}")

                audio_data = self._get_audio_data(chunk)
                if audio_data:
                    yield self._decode_audio_data(audio_data)

    def _iter_legacy_tts_audio(self, text):
        from dashscope.audio.tts import SpeechSynthesizer
        from dashscope.audio.tts.speech_synthesizer import ResultCallback

        audio_frames = []

        class AudioCallback(ResultCallback):
            def on_event(self, result):
                audio_frame = result.get_audio_frame()
                if audio_frame:
                    audio_frames.append(audio_frame)

            def on_error(self, response):
                logger.bind(tag=TAG).error(f"Qwen TTS回调错误: {response}")

        dashscope.api_key = self.api_key
        kwargs = {
            "model": self.model,
            "text": text,
            "voice": self.voice,
            "format": self.format,
            "sample_rate": self.sample_rate,
            "callback": AudioCallback(),
            "stream": True,
        }
        if self.instructions:
            kwargs["instructions"] = self.instructions
            kwargs["optimize_instructions"] = True

        result = SpeechSynthesizer.call(**kwargs)

        if audio_frames:
            for audio_frame in audio_frames:
                yield audio_frame
            return

        audio_data = result.get_audio_data() if result else None
        if audio_data:
            yield audio_data

    def _get_audio_data(self, chunk):
        if isinstance(chunk, (bytes, bytearray)):
            return bytes(chunk)

        if isinstance(chunk, dict):
            output = chunk.get("output") or {}
            audio = output.get("audio") or {}
            if isinstance(audio, dict):
                return audio.get("data") or audio.get("audio")
            return audio

        output = getattr(chunk, "output", None)
        if isinstance(output, (bytes, bytearray)):
            return bytes(output)
        if isinstance(output, dict):
            audio = output.get("audio") or {}
            if isinstance(audio, dict):
                return audio.get("data") or audio.get("audio")
            return audio

        audio = getattr(output, "audio", None)
        if isinstance(audio, dict):
            return audio.get("data") or audio.get("audio")
        return getattr(audio, "data", None)

    def _decode_audio_data(self, audio_data):
        if isinstance(audio_data, (bytes, bytearray)):
            return bytes(audio_data)
        return base64.b64decode(audio_data)

    def to_tts(self, text: str) -> list:
        text = MarkdownCleaner.clean_markdown(text)
        audio_data = []
        self.pcm_buffer.clear()
        frame_bytes = int(self.sample_rate * 1 * 60 / 1000 * 2)

        try:
            for audio_bytes in self._iter_qwen_tts_audio(text):
                self.pcm_buffer.extend(audio_bytes)
                while len(self.pcm_buffer) >= frame_bytes:
                    frame = bytes(self.pcm_buffer[:frame_bytes])
                    del self.pcm_buffer[:frame_bytes]
                    self.opus_encoder.encode_pcm_to_opus_stream(
                        frame,
                        end_of_stream=False,
                        callback=lambda opus: audio_data.append(opus),
                    )

            if self.pcm_buffer:
                self.opus_encoder.encode_pcm_to_opus_stream(
                    bytes(self.pcm_buffer),
                    end_of_stream=True,
                    callback=lambda opus: audio_data.append(opus),
                )
                self.pcm_buffer.clear()

            return audio_data
        except Exception as e:
            logger.bind(tag=TAG).error(f"Qwen TTS生成音频数据失败: {str(e)}")
            return []

    def audio_to_pcm_data_stream(self, audio_file_path, callback=None):
        from core.utils.util import audio_to_data_stream

        return audio_to_data_stream(
            audio_file_path,
            is_opus=False,
            callback=callback,
            sample_rate=self.sample_rate,
            opus_encoder=None,
        )

    def audio_to_opus_data_stream(self, audio_file_path, callback=None):
        from core.utils.util import audio_to_data_stream

        return audio_to_data_stream(
            audio_file_path,
            is_opus=True,
            callback=callback,
            sample_rate=self.sample_rate,
            opus_encoder=self.opus_encoder,
        )

    async def close(self):
        await super().close()
        if hasattr(self, "opus_encoder"):
            self.opus_encoder.close()
