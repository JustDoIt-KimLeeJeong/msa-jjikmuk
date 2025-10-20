
from kafka import KafkaConsumer
from app.settings import Settings, KafkaSettings, KAFKA_SUB_TOPICS
from aiokafka import AIOKafkaConsumer
import asyncio


async def run_consumer(consumer) : 
    await consumer.start()
    try:
        # Consume messages
        async for msg in consumer:
            print("consumed: ", msg.topic, msg.partition, msg.offset,
                  msg.key, msg.value, msg.timestamp)
    finally:
        # Will leave consumer group; perform autocommit if enabled.
        await consumer.stop()

