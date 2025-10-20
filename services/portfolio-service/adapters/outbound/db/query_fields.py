from app.db.models import Position, Balance
from typing import Mapping



PORTFOLIO_POSITION_FIELDS = {
    "symbol" : Position.symbol, 
    "qty": Position.qty,  
    "avg_price": Position.avg_price,  
    "realized_pnl": Position.realized_pnl

}

PORTFOLIO_BALANCE_FIELDS = {
    "available" : Balance.available, 
    "reserved" : Balance.reserved, 
}

