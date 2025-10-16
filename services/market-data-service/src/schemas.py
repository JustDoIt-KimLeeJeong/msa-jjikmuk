from typing import Literal, List, Optional
from pydantic import BaseModel, Field
from datetime import datetime

###symbols
class Rules(BaseModel):
    maxSymbols: int = 30
    defaultWatchlist: List[str]
    intervals: List[Literal["1m", "1d", "1w"]]
    defaultQuoteLevel: int = Field(1, ge=1, le=2)

class SymbolItem(BaseModel):
    symbol: str
    name: str
    market: Literal["KOSPI"]
    isin: str
    currency: Literal["KRW"]
    tickSize: int
    priceLimitPct: int
    lotSize: int
    displayPrecision: int
    active: bool = True

class SymbolsResponse(BaseModel):
    version: int
    updatedAt: datetime
    rules: Rules
    symbols: List[SymbolItem]

###prices
class ErrorItem(BaseModel):
    symbol : str
    code : str

class PriceItem(BaseModel):
    symbol : str
    name : str
    last : float
    chgPct: float = Field(..., description="전일 대비율(%)")
    ts : int 

class PriceListResponse(BaseModel):
    items: List[PriceItem]
    errors: List[ErrorItem] = []

class PriceResponse(PriceItem):
    pass