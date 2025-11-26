from fastapi import APIRouter
from sqlalchemy import text
from infra.db import SessionLocal
from adapters.kis.client import fetch_price_single

router = APIRouter(prefix="/dev/kis", tags=["dev-kis"])


@router.post("/save-price/{symbol}")
async def save_price(symbol: str):
    # 1) KIS 단건 호출
    prices = await fetch_price_single(symbol)

    if prices.errors:
        return prices  # 에러 그대로 반환

    item = prices.items[0]

    # 2) DB 저장
    db = SessionLocal()
    try:
        sql = text(
            """
            INSERT INTO kis_price_log
              (symbol, name, last, chg_pct, ts_ms, raw_json)
            VALUES
              (:symbol, :name, :last, :chg_pct, :ts_ms, :raw_json)
            """
        )
        db.execute(
            sql,
            {
                "symbol": item.symbol,
                "name": item.name,
                "last": item.last,
                "chg_pct": item.chgPct,
                "ts_ms": item.ts,
                "raw_json": prices.model_dump_json(),
            },
        )
        db.commit()
        return {"saved": True, "item": item}
    except Exception as e:
        db.rollback()
        return {"saved": False, "error": str(e)}
    finally:
        db.close()
