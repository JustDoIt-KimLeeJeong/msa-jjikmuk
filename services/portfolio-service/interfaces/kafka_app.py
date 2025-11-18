from faststream import FastStream
from adapters.inbound.events.consumer import broker

kafka_app = FastStream(broker)