from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from app.db.engine import get_session_depends
from domain.usecases.http_usecase import get_portfolio, update_balance

router = APIRouter(prefix="/api", tags=["balance"])

@router.get("/{user_id}")
async def get_balance(user_id: int, session: AsyncSession = Depends(get_session_depends)):
    return await get_portfolio(user_id, session)

@router.post("/{user_id}/deposit")
async def deposit_balance(user_id: int, deposit: int, session: AsyncSession = Depends(get_session_depends)):
    return await update_balance(user_id, deposit, session)
