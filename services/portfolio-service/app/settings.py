from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional, ClassVar


# kafka용 
import os
from dataclasses import dataclass


# 전체 Settings

class Settings:
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



# Kafka settings 



# ── 유틸: 컨테이너 여부 감지 (docker exec/compose 환경이면 True일 가능성 높음)
def _in_container() -> bool:
    return os.path.exists("/.dockerenv") or os.getenv("IN_CONTAINER") == "1"

# ── 공통 기본값: 컨테이너 내부는 kafka:9092, 호스트는 localhost:19092
_DEFAULT_BOOTSTRAP = "kafka:9092" if _in_container() else "localhost:19092"

@dataclass(frozen=True)
class KafkaProducerConfig:
    # bootstrap_servers: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS", _DEFAULT_BOOTSTRAP)
    client_id: str = os.getenv("KAFKA_CLIENT_ID", "portfolio-service")
    acks: str = os.getenv("KAFKA_ACKS", "all")                   # all, 1, 0
    linger_ms: int = int(os.getenv("KAFKA_LINGER_MS", "5"))      # 배치 지연
    retries: int = int(os.getenv("KAFKA_RETRIES", "5"))
    retry_backoff_ms: int = int(os.getenv("KAFKA_RETRY_BACKOFF_MS", "100"))
    compression_type : str = os.getenv("KAFKA_COMPRESSION_TYPE")
    # 보안 쓰면 아래 주석 해제해 사용security_protocol: str = os.getenv("KAFKA_SECURITY_PROTOCOL", "PLAINTEXT")
    # sasl_mechanism: str | None = os.getenv("KAFKA_SASL_MECHANISM") or None
    # sasl_username: str | None = os.getenv("KAFKA_SASL_USERNAME") or None
    # sasl_password: str | None = os.getenv("KAFKA_SASL_PASSWORD") or None

    # 
@dataclass(frozen=True)
class KafkaConsumerConfig:
    # bootstrap_servers: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS", _DEFAULT_BOOTSTRAP)
    group_id: str = os.getenv("KAFKA_CONSUMER_GROUP", "pf-dev")
    auto_offset_reset: str = os.getenv("KAFKA_AUTO_OFFSET_RESET", "earliest")  # earliest|latest
    enable_auto_commit: bool = os.getenv("KAFKA_ENABLE_AUTO_COMMIT", "true").lower() == "true"
    auto_commit_interval_ms: int = int(os.getenv("KAFKA_AUTO_COMMIT_INTERVAL_MS", "5000"))
    max_poll_records: int = int(os.getenv("KAFKA_MAX_POLL_RECORDS", "100"))
    session_timeout_ms: int = int(os.getenv("KAFKA_SESSION_TIMEOUT_MS", "10000"))
    heartbeat_interval_ms: int = int(os.getenv("KAFKA_HEARTBEAT_INTERVAL_MS", "3000"))
    poll_timeout_ms: int = int(os.getenv("KAFKA_POLL_TIMEOUT_MS", "1000"))
    fetch_max_bytes: ClassVar[int] = 25 * 1024 * 1024
    # 보안 설정(필요 시)
    security_protocol: str = os.getenv("KAFKA_SECURITY_PROTOCOL", "PLAINTEXT")
    sasl_mechanism: str | None = os.getenv("KAFKA_SASL_MECHANISM") or None
    sasl_username: str | None = os.getenv("KAFKA_SASL_USERNAME") or None
    sasl_password: str | None = os.getenv("KAFKA_SASL_PASSWORD") or None

@dataclass(frozen=True)
class KafkaTopics:
    portfolio: str = "portfolio.events"
    dlq: str = "portfolio.dlq"

@dataclass(frozen=True)
class Kafka_Settings:
    env: str = os.getenv("APP_ENV", "local")
    log_level: str = os.getenv("LOG_LEVEL", "INFO")
    bootstrap_servers: str = _DEFAULT_BOOTSTRAP
    kafka_producer: KafkaProducerConfig = KafkaProducerConfig()
    kafka_consumer: KafkaConsumerConfig = KafkaConsumerConfig()
    topics: KafkaTopics = KafkaTopics()
# 모듈 import 시 한 번만 로드해서 전역으로 재사용
settings = Kafka_Settings()
