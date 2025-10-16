# src/adapters/kis.py
from typing import Optional, Dict
from utils.time import now_ms
from repositories.symbols import get_name

async def fetch_l1_quote(http, symbol: str) -> Optional[Dict]:
    # TODO: 실제 KIS API 연동으로 교체
    return {
        "symbol": symbol,
        "name": get_name(symbol),
        "bid": 70000.0,
        "ask": 70100.0,
        "bidSize": 1200.0,
        "askSize": 950.0,
        "ts": now_ms(),
    }
