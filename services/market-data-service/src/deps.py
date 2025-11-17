#파일, 디렉터리 경로를 os 독립적으로 다룸.
import pathlib
#비동기 컨텍스트(async with)용 생명주기 관리자를 쉽게 정의
from contextlib import asynccontextmanager
from fastapi import Request
#비동기 HTTP 클라이언트
import httpx
from config import Settings
from utils.io import load_json
from infra.redis import create_redis
from repositories import symbols as symbols_repo
from repositories import prices as prices_repo

DATA_DIR = pathlib.Path(__file__).parent / "data"


@asynccontextmanager
async def lifespan(app):
    # 1) settings 인스턴스
    app.state.settings = Settings()

    app.state.redis = create_redis(app.state.settings)

    symbols_obj = load_json(DATA_DIR / "symbols.json")
    app.state.symbols = symbols_obj
    name_map = {s["symbol"]: s["name"] for s in symbols_obj.get("symbols", [])}
    app.state.name_map = name_map
    symbols_repo.prime(name_map)
    
    # 4) prices 로드 → repo prime
    try:
        prices = load_json(DATA_DIR / "prices.json")
    except FileNotFoundError:
        prices = {"last": {}, "chgPct": {}}
    prices_repo.prime(prices)

    try:
        yield
    finally:
        r = app.state.redis
        # 비동기/동기 클라이언트 호환 종료
        if hasattr(r, "aclose"):
            await r.aclose()
        elif hasattr(r, "close"):
            res = r.close()
            if hasattr(res, "__await__"):
                await res

def get_settings(request: Request) -> Settings:
    return request.app.state.settings

def get_redis(request: Request):
    return request.app.state.redis

async def get_http():
    async with httpx.AsyncClient(timeout=5.0) as client:
        yield client