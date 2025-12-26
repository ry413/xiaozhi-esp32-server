from typing import Dict, Any

from core.handle.textMessageHandler import TextMessageHandler
from core.handle.textMessageType import TextMessageType
from core.handle.receiveAudioHandle import startToChat


TAG = __name__


class DirectChatTextMessageHandler(TextMessageHandler):
    """DirectChatTextMessageHandler消息处理器"""

    @property
    def message_type(self) -> TextMessageType:
        return TextMessageType.DIRECT_CHAT

    async def handle(self, conn, msg_json: Dict[str, Any]) -> None:
        msg: str = msg_json.get("message", "")
        await startToChat(conn, msg)