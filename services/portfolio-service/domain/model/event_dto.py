from pydantic import BaseModel, ConfigDict, Field, field_validator
# import datetime


# Base Class들
class PurchaseSellEventEnvelope(BaseModel) : 
    model_config = ConfigDict(populate_by_name=True, frozen=True)
    event_id: str = Field(default=None, alias="eventId")
    order_id: str = Field(default=None, alias="orderId")
    user_id: int = Field(default=None, alias="userId")

    @field_validator("event_id", "order_id", "user_id") 
    def none_validation(cls, v) : 
        if v == None :
            raise ValueError("event/order/user id 는 None 일 수 없습니다")
        
    

class InternalFailEventEnvelope(BaseModel) : 
    user_id : int
    reason_code : str

    @field_validator("user_id") 
    def none_validation(cls, v) : 
        if v == None :
            raise ValueError("user id 는 None 일 수 없습니다")


# Base를 제외한 나머지

class Order(PurchaseSellEventEnvelope) : 
    symbol: str
    qty : int
    @field_validator("symbol", "qty") 
    def none_validation(cls, v) : 
        if v == None :
            raise ValueError("symbol과 qty 는 None 일 수 없습니다")
    
    @field_validator("qty") 
    def qty_validation(cls, v) : 
        if v <0 :
            raise ValueError("qty 는 0 이하 일 수 없습니다")

class Reject(PurchaseSellEventEnvelope) : 
    reason_code : str

class PositionUpdateFail(InternalFailEventEnvelope) : 
    symbol : str
    qty : int

class PriceSyncDegrade(InternalFailEventEnvelope) : 
    symbol : str 


