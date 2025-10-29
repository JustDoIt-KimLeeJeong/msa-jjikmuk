import json 
from pydantic import BaseModel
from typing import Optional
from datetime import datetime, timezone
from typing import Tuple, Dict



# 헤더는 dict[str,str] -> list[tuple[str, bytes]] 로 변환
def build_headers(event : str, correlation_id : str) -> list[Tuple[str, bytes]]:
    if event is None : 
        raise ValueError("event type cannot be None") 
    header: Dict[str, str] = {
        "event-type": event,
        "correlation_id" : correlation_id
    }
    return [(k, v.encode("utf-8")) for k, v in header.items()]

# 최종 메시지 바디: envelope + payload(by_alias) payload아직 추가 안함. 기능 추가해야 함 
def to_message_bytes(event: BaseModel) -> bytes:
    body = {
        "type": event,
        "occurred_at": datetime.now(timezone.utc).isoformat(),
        "payload": {"test_payload" : "test"},
    }
    return json.dumps(body, ensure_ascii=False).encode("utf-8")
