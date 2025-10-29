from kafka import KafkaProducer
from app.settings import settings
from pydantic import BaseModel
from serializer import build_headers, to_message_bytes
from domain.ports.event_publisher import EventPublisher

# send 를 어떤 로직으로 보내야 함? 모르겠엄.

class KafkaPublishEvent(EventPublisher) :
    def __init__(self) -> None : 
        settings = settings
        self.topic_default = settings.topics.portfolio
        self.producer = KafkaProducer(
            bootstrap_servers = settings._DEFAULT_BOOTSTRAP,
            client_id = settings.kafka_producer.client_id,
            acks = settings.kafka_producer.acks,
            retries = settings.kafka_producer.retries, 
            retry_backoff_ms = settings.kafka_producer.retry_backoff_ms,
            compression_type = settings.kafka_producer.compression_type
        )
    
    async def publish(self, event: BaseModel, event_type : str, correlation_id : str) :  # event 가BaseModel 
        headers = build_headers(event, correlation_id)
        value = to_message_bytes(event) # payload 가 지금 고정이어서 그런거임. 이거 입력 가능하도록 만들어야 함.
        
        future = self.producer.send(
            self.topic_default, 
            value=value,
            headers=headers
        )
        print(future.get())






    