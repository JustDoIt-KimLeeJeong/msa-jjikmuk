import json

def sse_event(event: str, data: dict) -> bytes:
    # SSE 프레임: "event: <name>\ndata: <json>\n\n"
    return f"event: {event}\ndata: {json.dumps(data, ensure_ascii=False)}\n\n".encode("utf-8")
