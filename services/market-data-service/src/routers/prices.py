from fastapi import APIRouter, Query, Response, Request, status
from fastapi.responses import StreamingResponse
from typing import List
import asyncio, json, random
from schemas import PriceItem, PriceListResponse, PriceResponse, ErrorItem
from deps import now_ms

router = APIRouter(tags=["prices"])

def _max_symbols(request: Request) -> int:
    return int(request.app.state.symbols["rules"].get("maxSymbols", 30))

def _resolve_prices(request: Request, symbols: List[str]) -> PriceListResponse:
    name_map = request.app.state.name_map
    ps = request.app.state.price_state
    last_map = ps.get("last", {})
    chg_map = ps.get("chgPct", {})

    items, errors = [], []
    for sym in symbols:
        name, last, chg = name_map.get(sym), last_map.get(sym), chg_map.get(sym)
        if name is None or last is None or chg is None:
            errors.append(ErrorItem(symbol=sym, code="SYMBOL_NOT_FOUND"))
            continue
        items.append(PriceItem(symbol=sym, name=name, last=float(last), chgPct=float(chg), ts=now_ms()))
    return PriceListResponse(items=items, errors=errors)



@router.get("/prices", response_model=PriceListResponse)
async def get_prices(request: Request, response: Response,
    symbols: str = Query(..., description="CSV, 최대 30개"),
):
    response.headers["Cache-Control"] = "no-store"
    sym_list = [s.strip() for s in symbols.split(",") if s.strip()]
    if not sym_list:
        return PriceListResponse(items=[], errors=[])
    if len(sym_list) > _max_symbols(request):
        return PriceListResponse(items=[], errors=[ErrorItem(symbol="*", code="TOO_MANY_SYMBOLS")])
    return _resolve_prices(request, sym_list)

## stream이 /prices/{symbol}보다 위에 있어야함.
# 먼저 등록된 길이 먼저 잡힘. {symbols}이런게 동적 경로인데 이건 등록 상관없이 뭐든 잡아먹음
# 그래서 동적 주소가 위에 있을 경우, 요청이 stream으로 오면 stream주소로 안가고 동적주소로 먹힘.
# 이런거 주의해야함.
@router.get("/prices/stream")
async def stream_prices(request: Request, response: Response,
    symbols: str = Query("", description="CSV, 최대 30개"),
):
    response.headers["Cache-Control"] = "no-store"
    sym_list = [s.strip() for s in symbols.split(",") if s.strip()]
    if not sym_list:
        sym_list = request.app.state.symbols["rules"]["defaultWatchlist"][:_max_symbols(request)]
    if len(sym_list) > _max_symbols(request):
        sym_list = sym_list[:_max_symbols(request)]
    return StreamingResponse(_sse_generator(request, sym_list), media_type="text/event-stream")


@router.get("/prices/{symbol}", response_model=PriceResponse)
async def get_price_by_symbol(request: Request, response: Response, symbol: str):
    response.headers["Cache-Control"] = "no-store"
    res = _resolve_prices(request, [symbol])
    if not res.items:
        response.status_code = status.HTTP_404_NOT_FOUND
        raise ValueError(f"{symbol} not found")
    return res.items[0]

def _sse_encode(payload: dict) -> bytes:
    return f"data: {json.dumps(payload, ensure_ascii=False)}\n\n".encode("utf-8")

async def _sse_generator(request: Request, symbols: List[str]):
    ps = request.app.state.price_state
    last_map, chg_map = ps.setdefault("last", {}), ps.setdefault("chgPct", {})
    while True:
        for s in symbols:
            if s in last_map:
                base = float(last_map[s]) + random.uniform(-0.3, 0.3)
                last_map[s] = round(max(1.0, base), 2)
            if s in chg_map:
                chg_map[s] = round(float(chg_map[s]) + random.uniform(-0.05, 0.05), 2)
        yield _sse_encode(_resolve_prices(request, symbols).model_dump())
        await asyncio.sleep(1)



