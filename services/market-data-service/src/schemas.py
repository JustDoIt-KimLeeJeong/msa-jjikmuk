from typing import Literal, List
from pydantic import BaseModel, Field
from datetime import datetime

Market = Literal["KOSPI", "KOSDAQ"]

class Rules(BaseModel):
    maxSymbols: int = 30
    defaultWatchlist: List[str]
    intervals: List[Literal["1m", "1d", "1w"]]
    defaultQuoteLevel: int = Field(1, ge=1, le=2)

class SymbolItem(BaseModel):
    symbol: str
    name: str
    market: Market
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