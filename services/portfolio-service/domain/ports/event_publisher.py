from pydantic import BaseModel

class EventPublisher(BaseModel)  : 
    async def publish(self, event_type : str, correlation_id : str) -> None: 
        pass