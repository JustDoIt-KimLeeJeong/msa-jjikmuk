from pydantic import BaseModel, ConfigDict, Field, field_validator
from datetime import datetime




"""
############################Inbound DTO######################################################
"""

####################### Base Class
class PurchaseSellEventEnvelope(BaseModel) : 
    model_config = ConfigDict(populate_by_name=True, frozen=True)
    event_id: str = Field(default=None, alias="eventId")
    order_id: str = Field(default=None, alias="orderId")
    user_id: int = Field(default=None, alias="userId")

class InternalFailEventEnvelope(BaseModel) : 
    user_id : int
    reason_code : str


####################### Specific Class


class Order(PurchaseSellEventEnvelope) : 
    symbol: str
    qty : int
    
    @field_validator("qty") 
    def qty_validation(cls, v) : 
        if v <0 :
            raise ValueError("qty 는 0 이하 일 수 없습니다")
        return v

class Reject(PurchaseSellEventEnvelope) : 
    reason_code : str

class PositionUpdateFail(InternalFailEventEnvelope) : 
    symbol : str
    qty : int

class PriceSyncDegrade(InternalFailEventEnvelope) : 
    symbol : str 


"""
############################Outbound DTO######################################################
"""

####################### Base Class

class OutboundEventEnvelop(BaseModel) : 
    event_id : str
    created_at : str
    order_id : str
    user_id : int
    

####################### Specific Class

class OrderPlace(OutboundEventEnvelop) : 
    symbol : str
    qty : int
    price : int # 삭제 필요?
    side : str # 삭제 필요?
    reserved_balance : int # 삭제 필요?
    reserved_qty : int # 삭제 필요?

class TradeExecuted(OutboundEventEnvelop) : 
    trade_id : str
    symbol : str
    price : int
    qty : int
    order_id : str
    fill_id : str
    side : str
    last_fill_qty : str
    fill_price : int
    fees : int
    filled_at : datetime


