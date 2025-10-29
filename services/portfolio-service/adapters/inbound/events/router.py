from kafka import KafkaConsumer
from app.settings import Settings, KafkaSettings, KAFKA_SUB_TOPICS
from aiokafka import AIOKafkaConsumer
import asyncio

async def run_consumer() : 
    topics = [t for t in KAFKA_SUB_TOPICS]
    consumer = AIOKafkaConsumer(
        *topics,
        bootstrap_servers = "localhost:9092", 
        group_id="", # group이 없으므로 어떻게 채워야 하지>?
        auto_offset_reset="earliest",
        #client_id=settings.KAFKA_CLIENT_ID + "-single", # client id 도 없지 않나.
    )

    return consumer


