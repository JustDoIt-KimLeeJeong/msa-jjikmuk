from app.settings import base_settings
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine, AsyncSession
from contextlib import asynccontextmanager



engine = create_async_engine(base_settings.PG_DSN, pool_size=10, pool_pre_ping=True)

session_maker =  async_sessionmaker(bind = engine, class_=AsyncSession, expire_on_commit=False)


@asynccontextmanager
async def get_session() -> AsyncSession : 
    
    async with session_maker() as session : 
        yield session 







