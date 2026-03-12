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
    EventDTOHandler("OrderPlaced", DTO.OrderPlace, Handler.order_placed), 
    EventDTOHandler("OrderCancelled", DTO.OutboundEventEnvelop, Handler.order_cancelled), 
    EventDTOHandler("OrderExpired", DTO.OutboundEventEnvelop, Handler.order_expired),
    EventDTOHandler("TradeExecuted", DTO.TradeExecuted, Handler.trade_executed)
]


def build_event_router() -> EventRouter : 
    router = EventRouter()
    
    for r in route_list : 
        router.add_router(r.event, r.DTOType, r.handler)

    return router

# def subscribed_event() -> list[str]:  
#     return [i.event for i in route_list]