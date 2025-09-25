from fastapi import APIRouter, Depends, status
from deps import get_redis

router = APIRouter()

@router.get("/health", status_code=status.HTTP_200_OK)
async def health(redis = Depends(get_redis)):
    # 최소 체크: 프로세스 살아있음
    result = {"ok": True}

    # 선택 체크: Redis 연결 상태
    try:
        pong = await redis.ping()
        result["redis"] = "up" if pong else "down"
    except Exception as e:
        result["redis"] = "down"
        result["error"] = str(e)

    # 의존성이 죽었을 때 503 반환하고 싶으면 아래 주석 해제
    # if result.get("redis") == "down":
    #     from fastapi import Response
    #     return Response(content=str(result), status_code=status.HTTP_503_SERVICE_UNAVAILABLE)

    return result