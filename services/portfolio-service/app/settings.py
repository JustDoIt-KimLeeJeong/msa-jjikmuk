from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional, ClassVar

class Settings(BaseSettings) :
    # app 전체 
    APP_NAME : ClassVar[str] = "portfolio"

    # DB
    PG_HOST : str
    PG_PORT : int
    PG_DB_NAME : str
    PG_PASSWORD : str
    PG_USER : str

    

    # env
    model_config = SettingsConfigDict(env_file='.env',  extra="ignore")

    @property
    def PG_DSN(self) -> str: 
        return f"postgresql+asyncpg://{self.PG_USER}:{self.PG_PASSWORD}@{self.PG_HOST}:{self.PG_PORT}/{self.PG_DB_NAME}"

settings = Settings()

KAFKA_SUB_TOPICS = (
  "portfolio.OrderPlaced.v1,"
  "portfolio.TradeExecuted.v1,"
  "portfolio.PurchaseReserved.v1"
)


class KafkaSettings(BaseSettings) : 

    model_config = SettingsConfigDict(env_file=".env",  extra="ignore")
    KAFKA_CONSUMER_INSTANCES: int = 3
    # 구독 방식 ②: 패턴(선택) — 지정하면 리스트 대신 패턴으로 subscribe
    KAFKA_SUB_PATTERN: Optional[str] = None
    # 예: r"^portfolio\.(TradeExecuted|PurchaseReserved)\.v\d+$"

    # "earliest" | "latest"
    KAFKA_START_POSITION: str = "latest"
