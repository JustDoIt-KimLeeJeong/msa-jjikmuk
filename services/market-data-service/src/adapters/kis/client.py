import httpx
from datetime import datetime, timezone
from config import Settings
from .auth import get_access_token
from schemas import PriceItem, PriceListResponse, ErrorItem

_settings = Settings()
BASE_URL = "https://openapi.koreainvestment.com:9443"

def now_ms() -> int:
    return int(datetime.now(timezone.utc).timestamp() * 1000)


async def fetch_price_single(symbol: str) -> PriceListResponse:
    access_token = await get_access_token()  #여기서 auth.py 사용

    url = f"{BASE_URL}/uapi/domestic-stock/v1/quotations/inquire-price"

    headers = {
        "content-type": "application/json; charset=utf-8",
        "authorization": f"Bearer {access_token}",
        "appkey": _settings.kis_appkey,
        "appsecret": _settings.kis_appsecret,
        "tr_id": "FHKST01010100" if _settings.kis_virtual else "HHKST01010100",
    }

    params = {
        "FID_COND_MRKT_DIV_CODE": "J", 
        "FID_INPUT_ISCD": symbol,
    }

    async with httpx.AsyncClient(timeout=5.0) as client:
        res = await client.get(url, headers=headers, params=params)
        res.raise_for_status()
        data = res.json()

    # KIS 공통 상태 확인
    if data.get("rt_cd") != "0":
        err = ErrorItem(symbol=symbol, code=data.get("msg_cd", "KIS_ERROR"))
        return PriceListResponse(items=[], errors=[err])

    output = data.get("output", {})

    # KIS 필드 → 내가 원하는 형태로 받아오기. 문서 참고
    last = float(output["stck_prpr"])
    chg_pct = float(output["prdy_ctrt"])
    name = output.get("hts_kor_isnm") or output.get("bstp_kor_isnm", "")

    item = PriceItem(
        symbol=symbol,
        name=name,
        last=last,
        chgPct=chg_pct,
        ts=now_ms(),
    )

    return PriceListResponse(items=[item], errors=[])
