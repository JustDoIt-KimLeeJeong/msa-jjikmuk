from app.settings import settings
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine, AsyncSession

engine = create_async_engine(settings.PG_DSN, pool_size=10, pool_pre_ping=True)

session_maker =  async_sessionmaker(bind = engine, class_=AsyncSession, expire_on_commit=False)


async def get_session() -> AsyncSession : 
    async with session_maker() as session : 
        yield session 







