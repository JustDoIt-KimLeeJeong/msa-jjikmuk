# config.py
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field

API_PREFIX = "/api/v1/market"


class Settings(BaseSettings):
    redis_url: str = Field("redis://localhost:6379/0", alias="REDIS_URL")

    # KIS
    # .env 에 KIS_BASE_URL 이 비어 있으면 기본값을 쓰고,
    # 채워 넣으면 그 값을 사용
    kis_base_url: str = Field(
        "https://openapi.koreainvestment.com:9443",
        alias="KIS_BASE_URL",
    )
    kis_appkey: str = Field(..., alias="KIS_APPKEY")
    kis_appsecret: str = Field(..., alias="KIS_APPSECRET")
    kis_virtual: bool = Field(True, alias="KIS_VIRTUAL")

    # DB
    db_host: str = Field("127.0.0.1", alias="DB_HOST")
    db_port: int = Field(3306, alias="DB_PORT")
    db_user: str = Field("app", alias="DB_USER")
    db_password: str = Field("app_pw", alias="DB_PASSWORD")
    db_name: str = Field("market_data", alias="DB_NAME")

    model_config = SettingsConfigDict(
        env_file="../.env",   # uvicorn 을 src에서 띄운다는 전제
        extra="ignore",
    )
