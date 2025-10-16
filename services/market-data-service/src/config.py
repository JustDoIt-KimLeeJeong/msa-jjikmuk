from pydantic_settings import BaseSettings, SettingsConfigDict

API_PREFIX = "/api/v1/market"

class Settings(BaseSettings):
    redis_url : str = "redis://localhost:6379/0"
    # redis_url 없으면 env 가져다 써라
    model_config = SettingsConfigDict(env_file='.env', extra="ignore")
