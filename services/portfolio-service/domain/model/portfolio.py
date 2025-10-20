from dataclasses import dataclass, field
from typing import List


# Web API 용
@dataclass
class Balance:
    available: int 
    reserved: int 

@dataclass
class Position:
    symbol: str
    qty: int
    avg_price: int 
    realized_pnl: int

@dataclass
class Portfolio:
    user_id: int
    balances: Balance
    positions: List[Position] = field(default_factory=list)

