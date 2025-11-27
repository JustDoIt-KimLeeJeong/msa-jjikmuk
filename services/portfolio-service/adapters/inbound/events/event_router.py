from pydantic import ValidationError
from typing import Dict
from app.db.engine import get_session

class EventRouter: 
    def __init__(self) : 
        self.routes : Dict[str] = {}

    def add_router(self, event_type : str, DTO, handler) : # DTO랑 handler는 타입을 어떻게 지정해야 할지 모르겠어서 냅둠.
        self.routes[event_type] = (DTO, handler)
    
    async def get_router(self, event_type : str, payload : dict, headers : dict) : 
        dto_class, handler = self.routes.get(event_type)
        
        if not dto_class or not handler : 
            return 
        
        
        dto = dto_class(**payload)
        async with get_session() as session :   
            try : 
                await handler(session,event_type, dto, headers) # 이게 되는구나.... 테스트 해보고 에러 뜨면 변경
                await session.commit()
            except Exception as e : 
                print(f"error during handler or session commit : {e}")
         
        
    

