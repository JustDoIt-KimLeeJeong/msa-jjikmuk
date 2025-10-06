from fastapi import APIRouter, Depends


router = APIRouter(prediix="portfolio")

@router.get("/me")
async def get_portfolio(user: int)  : # -> 출력 형태 잡아서 reference 주자
    # DB 에서 데이터를 가져와야 함. 
    
    pass
