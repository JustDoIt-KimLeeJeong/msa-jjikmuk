from fastapi import APIRouter, Request, Response, Depends, Query, status
#sse 제너레이터 출력.
from starlette.responses import StreamingResponse
from typing import Dict
from schemas import QuoteResponse
from deps import get_http
from adapters.kis import fetch_l1_quote
from . import sse_event
#비동기 반복, 지연, 취소 제어용 표준 라이브러리
import asyncio

router = APIRouter(tags=["quotes"])
POLL_INTERVAL_SEC = 1.0

@router.get("/stream")
async def stream_quotes(request: Request, response: Response, 
                        symbols: str = Query(...), http=Depends(get_http)):
    
    response.headers["Cache-Control"] = "no-store"
    syms = [s.strip() for s in symbols.split(',') if s.strip()]
    last : Dict[str, QuoteResponse] = {}
    async def gen():
        for s in syms:
            q = await fetch_l1_quote(http, s)
            if q:
                last[s] = QuoteResponse(**q); yield sse_event("quote", q)
        while not await request.is_disconnected():
            for s in syms:
                q = await fetch_l1_quote(http, s)
                if not q: continue
                curr = QuoteResponse(**q); prev = last.get(s)
                if not prev or (curr.bid, curr.ask, curr.bidSize, curr.askSize) != (prev.bid, prev.ask, prev.bidSize, prev.askSize):
                    last[s] = curr; yield sse_event("quote", q)
            await asyncio.sleep(POLL_INTERVAL_SEC)
    return StreamingResponse(gen(), media_type="text/event-stream")


@router.get("/{symbol}", response_model=QuoteResponse)
async def get_quote_by_symbol(response: Response, symbol: str, http=Depends(get_http)):
    response.headers["Cache-Control"] = "no-store"
    q = await fetch_l1_quote(http, symbol)
    if not q:
        response.status_code = status.HTTP_404_NOT_FOUND
        raise ValueError(f"{symbol} not found")
    return q