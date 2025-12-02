import domain.model.event_dto as DTO
import domain.usecases.kafka_event_handler as Handler
from adapters.inbound.events.event_router import EventRouter
from typing import Type, Callable
from dataclasses import dataclass 

@dataclass
class EventDTOHandler :
    event : str
    DTOType : Type
    handler : Callable
    


route_list = [
    EventDTOHandler("OrderPlaced", DTO.OrderPlace,Handler.order_placed), 
    EventDTOHandler("OrderCancelled",DTO.OutboundEventEnvelop, Handler.order_cancelled), 
    EventDTOHandler("TradeExecuted", DTO.TradeExecuted, Handler.trade_executed), 
    EventDTOHandler("Test", DTO.OutboundEventEnvelop, Handler.test),
    # router.add_router("OrderExpired", DTO.OutboundEventEnvelop, Handler.) # 아직 미작성된 코드
]

def build_event_router() -> EventRouter : 
    router = EventRouter()
    
    for r in route_list : 
        router.add_router(r.event, r.DTOType, r.handler)

    return router

def subscribed_event() -> list[str]:  
    return [i.event for i in route_list]