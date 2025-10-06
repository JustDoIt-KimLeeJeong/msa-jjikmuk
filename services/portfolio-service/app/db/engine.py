from sqlalchemy import create_engine
from ..settings import Settings
from sqlalchemy.ext.asyncio import async_sessionmaker, AsyncSession, create_async_engine

# 테스트 출력 
print(Settings.PG_DSN())

engine = create_async_engine(Settings.PG_DSN(), pool_size=10, pre_ping=True)

Session = AsyncSession(bind=engine)
session =  async_sessionmaker(sync_session_class=Session)








