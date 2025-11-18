from pydantic import ValidationError
from typing import Dict

class EventRouter: 
    def __init__(self) : 
        self.routes : Dict[str] = {}

    def add_router(self, event_type : str, DTO, handler) : # DTO랑 handler는 타입을 어떻게 지정해야 할지 모르겠어서 냅둠.
        self.routes[event_type] = (DTO, handler)
    
    async def get_router(self, event_type : str, payload : dict) : 
        dto, handler = self.routes.get(event_type)
        
        if not dto or not handler : 
            return 
        
        
        dto(**payload)

        await handler(event_type) # 이게 되는구나.... 테스트 해보고 에러 뜨면 변경 
        
    

