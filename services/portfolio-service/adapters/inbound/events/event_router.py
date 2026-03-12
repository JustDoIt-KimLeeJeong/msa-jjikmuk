from pydantic import ValidationError
from typing import Dict
from app.db.engine import get_session
from domain.usecases.kafka_event_handler import failure_outbox_save
from datetime import datetime

class EventRouter: 
    def __init__(self) : 
        self.routes : Dict[str] = {}

    def add_router(self, event_type : str, DTO, handler) :
        """
        Docstring for add_router
        
        :param self: class self
        :param event_type: Kafka Event name to call handler (type : str)
        :param DTO: DTO to update all 
        :param handler: Description
        """
        self.routes[event_type] = (DTO, handler)
    
    async def get_router(self, event_type : str, payload : dict, headers : dict) : 
        """
        Docstring for get_router
        
        :param self: class self
        :param event_type: Kafka Event name from Consumer. Use to call hanlder (type : str)
        :param payload: Payload from Kafka Consumer (type : dict)
        :param headers: header from Kafka Consumer. need to contain event_type and correlation_id (type : dict)
        """
        
        if "correlation_id" not in headers:
            print(f"Missing correlation_id in headers for event: {event_type}")
            return
        
        dto_class, handler = self.routes.get(event_type)
        
        if not dto_class or not handler : 
            return 
        
        
        dto = dto_class(**payload)
        async with get_session() as session :   
            try : 
                await handler(session,event_type, dto, headers)
                await session.commit()
            except Exception as e : 
                print(f"error during handler or session commit: {e}")  
                async with get_session() as exception_session:
                    try:
                        await failure_outbox_save(
                            user_id=dto.user_id,
                            event_id=dto.event_id,
                            original_event_type=event_type,
                            correlation_id = headers.correlation_id,
                            order_id=getattr(dto, 'order_id', None),
                            symbol=getattr(dto, 'symbol', None),
                            qty=getattr(dto, 'qty', None),
                            reason_code=str(e),
                            session=exception_session
                        )
                        await exception_session.commit()
                    except Exception as exception_e:
                        print(f"failure_outbox_save error: {exception_e}")
                        
                
                
        
    

