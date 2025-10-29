from fastapi import APIRouter, Depends
import domain.usecases.http_usecase as usecase
from app.db.engine import get_session
import schemas

router = APIRouter(prefix="/portfolio")

@router.get("/me")
async def get_portfolio(user_id: int)   : # -> 출력 형태 잡아서 reference 주자
    # DB 에서 데이터를 가져와야 함.
    portfolio = usecase.get_portfolio(user_id, Depends = get_session(), ) 
    return portfolio

