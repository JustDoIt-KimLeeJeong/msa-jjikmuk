import asyncio, pathlib
from typing import AsyncIterator, Tuple, List
from utils.io import load_json
from schemas import CandleItem, Interval

DATA_DIR = pathlib.Path(__file__).resolve().parents[1] / "data" / "candles"

def _file(symbol: str, interval: Interval) -> pathlib.Path:
    return DATA_DIR / f"{symbol}_{interval}.json"

def _load(symbol: str, interval: Interval) -> list[dict]:
    fp = _file(symbol, interval)
    if not fp.exists():
        return []
    data = load_json(fp)  # 이미 프로젝트에 있는 util 사용
    # 오름차순 가정. 필요시 정렬 보강: data.sort(key=lambda x: x["ts"])
    return data

async def get_candles(symbol: str, interval: Interval,
                      start: str | None, end: str | None, limit: int
                     ) -> Tuple[List[CandleItem], list[dict]]:
    raw = _load(symbol, interval)
    if start:
        raw = [r for r in raw if r["ts"] >= start]
    if end:
        raw = [r for r in raw if r["ts"] <= end]
    if limit:
        raw = raw[:limit]
    items = [CandleItem(**r) for r in raw]
    errors: list[dict] = [] if items else [{"symbol": symbol, "code": "NO_DATA"}]
    return items, errors

async def subscribe_minute_close(symbols: list[str], interval: Interval
                               ) -> AsyncIterator[dict]:
    buffers = {s: _load(s, interval) for s in symbols}
    idx = {s: 0 for s in symbols}
    while True:
        progressed = False
        for s in symbols:
            i = idx[s]
            buf = buffers[s]
            if i < len(buf):
                yield {"symbol": s, "interval": interval, **buf[i]}
                idx[s] += 1
                progressed = True
                await asyncio.sleep(0.4)  # 데모용
        if not progressed:
            await asyncio.sleep(1.0)
