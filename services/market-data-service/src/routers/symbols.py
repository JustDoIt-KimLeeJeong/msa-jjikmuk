from fastapi import APIRouter, Request
from schemas import SymbolsResponse

router = APIRouter(prefix="/api/v1/market")

@router.get("/symbols", response_model=SymbolsResponse)
async def get_symbols(request: Request, market: str | None = None):
    data = request.app.state.symbols
    if market:
        data = {
            **data,
            "symbols": [s for s in data["symbols"] if s["market"] == market]
        }

    return data
