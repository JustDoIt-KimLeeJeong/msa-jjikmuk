from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from config import Settings

settings = Settings()

# DSN(Data Source Name)
DSN = (
    f"mysql+pymysql://{settings.db_user}:{settings.db_password}"
    f"@{settings.db_host}:{settings.db_port}/{settings.db_name}"
    "?charset=utf8mb4"
)

# 엔진 생성
engine = create_engine(
    DSN,
    pool_pre_ping=True,     # MySQL 연결 끊김 방지
    future=True,            # SQLAlchemy 2.x 스타일
)

# 세션 팩토리(작업 단위)
SessionLocal = sessionmaker(
    bind=engine,
    autocommit=False,
    autoflush=False,
    expire_on_commit=False,
)
