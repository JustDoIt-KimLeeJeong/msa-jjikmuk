import httpx
import time
from dataclasses import dataclass
from config import Settings

_settings = Settings()

BASE_URL = "https://openapi.koreainvestment.com:9443"

@dataclass
class TokenState:
    access_token: str | None = None
    expires_at: float = 0.0  # epoch 초

_token_state = TokenState()


@dataclass
class RequestBody:
    grant_type: str
    appkey: str
    appsecret: str


@dataclass
class ResponseBody:
    access_token: str
    token_type: str
    expires_in: float
    access_token_token_expired: str


async def _request_new_token() -> ResponseBody:    
    url = f"{BASE_URL}/oauth2/tokenP"
    body = RequestBody(
        grant_type="client_credentials",
        appkey=_settings.kis_appkey,
        appsecret=_settings.kis_appsecret,
    )

    async with httpx.AsyncClient(timeout=5.0) as client:
        res = await client.post(url, json=body.__dict__)
        res.raise_for_status()
        data = res.json()

    # 여기서 data 구조를 KIS 문서 보고 맞춰야 함
    rb = ResponseBody(
        access_token=data["access_token"],
        token_type=data["token_type"],
        expires_in=float(data["expires_in"]),
        access_token_token_expired=data["access_token_token_expired"],
    )
    return rb


async def get_access_token() -> str:
    now = time.time()
    if _token_state.access_token and _token_state.expires_at - 60 > now:
        return _token_state.access_token

    rb = await _request_new_token()
    _token_state.access_token = rb.access_token
    _token_state.expires_at = now + rb.expires_in
    return rb.access_token
