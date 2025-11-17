from fastapi import APIRouter, Query, Response, status, Depends
#sse 라이브러리(헤더설정, 연결유지, 재연결 등 자동관리해줌)
from sse_starlette.sse import EventSourceResponse
from schemas import Interval, CandleResponse
from routers import sse_event
from deps import get_settings
from adapters import candles_mock  # 모킹 단계

router = APIRouter(tags=["candles"])

@router.get("/{symbol}", response_model=CandleResponse)
async def get_candles(symbol: str,
    interval: Interval = Query("1m"),
    start: str | None = Query(None, description="ISO8601 UTC"),
    end: str | None = Query(None, description="ISO8601 UTC"),
    limit: int = Query(500, le=5000),
    resp: Response = None,
    settings = Depends(get_settings),
):
    # settings는 이후 KIS 전환 시 어댑터 스위치에 사용
    items, errors = await candles_mock.get_candles(symbol, interval, start, end, limit)
    if errors:
        resp.status_code = status.HTTP_206_PARTIAL_CONTENT
    return {"symbol": symbol, "interval": interval, "items": items, "errors": errors or None}

@router.get("/stream")
async def stream(
    interval: Interval = Query("1m"),
    symbols: str = Query(..., description="comma separated"),
    settings = Depends(get_settings),
):
    symbols_list = [s.strip() for s in symbols.split(",") if s.strip()]

    async def gen():
        async for candle in candles_mock.subscribe_minute_close(symbols_list, interval):
            yield sse_event(event=f"Candle_{interval}", data=candle)

    return EventSourceResponse(gen(), media_type="text/event-stream")
