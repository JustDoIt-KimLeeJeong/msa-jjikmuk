
from app.settings import settings
from app.kafka_router import build_event_router, subscribed_event
from adapters.inbound.events.event_router import EventRouter
from faststream import FastStream
from faststream.kafka import KafkaBroker, KafkaMessage


broker = KafkaBroker(
    bootstrap_servers=settings.bootstrap_servers
)
app = FastStream(broker)

router = build_event_router()
events = subscribed_event()

def header(headers: dict[str, bytes] | None, key: str) -> str | None:
    if not headers: 
        return None
    v = headers.get(key)
    return v

@broker.subscriber(*events, 
                   group_id=settings.kafka_consumer.group_id, 
                #    enable_auto_commit= False,
                   auto_offset_reset= settings.kafka_consumer.auto_offset_reset,  
                   max_poll_records= settings.kafka_consumer.max_poll_records,
                   session_timeout_ms= settings.kafka_consumer.session_timeout_ms,
                   heartbeat_interval_ms= settings.kafka_consumer.heartbeat_interval_ms,
                   fetch_max_bytes= settings.kafka_consumer.fetch_max_bytes)





async def run_consumer(payload : dict, message: KafkaMessage ):
    headers = message.headers
    event_type = header(headers, "event_type")
    if not event_type : 
        print("Missing Event Type. Body : {body}")
    
    try :
        await router.get_router(event_type, payload)

    except Exception as e : 
        print(f"Error while fetching Router. event : {event_type}, Error : {e}") 
    