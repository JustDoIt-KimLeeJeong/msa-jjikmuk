from pydantic import BaseModel, ConfigDict, Field
# import datetime


# Base Class들
class PurchaseSellEventEnvelope(BaseModel) : 
    model_config = ConfigDict(populate_by_name=True, frozen=True)
    event_id: str = Field(default=None, alias="eventId")
    order_id: str = Field(default=None, alias="orderId")
    user_id: int = Field(default=None, alias="userId")

class InternalFailEventEnvelope(BaseModel) : 
    user_id : int
    reason_code : str


# Base를 제외한 나머지

class Order(PurchaseSellEventEnvelope) : 
    symbol: str
    qty : int

class Reject(PurchaseSellEventEnvelope) : 
    reason_code : str

class PositionUpdateFail(InternalFailEventEnvelope) : 
    symbol : str
    qty : int

class PriceSyncDegrade(InternalFailEventEnvelope) : 
    symbol : str 


