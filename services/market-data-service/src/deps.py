import json, pathlib
import redis.asyncio as redis
#redis 연결, 종료 보장.
from contextlib import asynccontextmanager
# FastAPI의 Request 객체 → 요청 중 app.state 같은 전역 상태 접근 가능
from fastapi import Request
from config import Settings

DATA_DIR = pathlib.Path(__file__).parent / "data"

# FastAPI 애플리케이션의 lifecycle(lifespan) 관리 함수
# 앱 시작 시: Redis 연결 생성 → app.state에 저장
# 앱 종료 시: Redis 연결을 닫아줌 (자원 누수 방지)
@asynccontextmanager
async def lifespan(app):
    # Settings 인스턴스를 생성해 app.state에 저장 (환경 설정 공유)
    app.state.settings = Settings()

    # Redis 클라이언트 연결 객체 생성
    # decode_responses=True → redis 값이 bytes가 아닌 str로 반환되도록 설정
    app.state.redis = redis.Redis.from_url(
        app.state.settings.redis_url,
        decode_responses=True
    )

    symbols_path = DATA_DIR/"symbols.json"
    with open(symbols_path, "r", encoding="utf-8") as f:
        app.state.symbols = json.load(f)
    try:
        # yield 지점까지 실행 → 앱이 동작하는 동안 이 상태 유지
        yield
    finally:
        # 앱이 종료될 때 Redis 연결 닫기
        await app.state.redis.close()


# FastAPI 의존성 주입 함수
# 요청 핸들러에서 Depends(get_settings)를 사용하면 Settings 객체를 꺼내 쓸 수 있음
def get_settings(request: Request) -> Settings:
    return request.app.state.settings

def get_redis(request: Request):
    return request.app.state.redis


