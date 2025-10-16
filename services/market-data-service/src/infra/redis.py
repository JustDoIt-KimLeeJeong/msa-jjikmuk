import redis.asyncio as redis
from config import Settings

def create_redis(settings: Settings):
    return redis.Redis.from_url(settings.redis_url, decode_responses=True)