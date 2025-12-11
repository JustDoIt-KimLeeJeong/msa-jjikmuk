from app.settings import base_settings
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine, AsyncSession
from contextlib import asynccontextmanager



engine = create_async_engine(base_settings.PG_DSN, pool_size=10, pool_pre_ping=True)

session_maker =  async_sessionmaker(bind = engine, class_=AsyncSession, expire_on_commit=False)



@asynccontextmanager
async def get_session() -> AsyncSession : 
    """
    Async Session for kafka
    
    :return : session (rtype : AsyncSession)
    """
    
    async with session_maker() as session : 
        yield session 


async def get_session_depends():
    """
    Docstring for get_session_depends
    :return : session (rtype : Session)
    
    """
    async with session_maker() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise



