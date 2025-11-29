import os
from typing import Optional
import aio_pika
import orjson

# AMQP adapter for publishing domain events to the exchange.
class Broker:
    def __init__(self, amqp_url: str, exchange_name: str = "ticketchief") -> None:
        self.amqp_url = amqp_url
        self.exchange_name = exchange_name
        self._connection: Optional[aio_pika.RobustConnection] = None
        self._channel: Optional[aio_pika.abc.AbstractChannel] = None
        self._exchange: Optional[aio_pika.abc.AbstractExchange] = None

    async def connect(self) -> None:
        self._connection = await aio_pika.connect_robust(self.amqp_url)
        self._channel = await self._connection.channel()
        self._exchange = await self._channel.declare_exchange(
            self.exchange_name, aio_pika.ExchangeType.TOPIC, durable=True
        )
        # Ensure a mirror/debug queue exists and receives all messages
        debug_queue = await self._channel.declare_queue("debug.events", durable=True)
        await debug_queue.bind(self._exchange, routing_key="#")

    async def close(self) -> None:
        if self._connection:
            await self._connection.close()

    async def publish(self, routing_key: str, message: dict) -> None:
        assert self._exchange is not None
        await self._exchange.publish(
            aio_pika.Message(body=orjson.dumps(message)), routing_key=routing_key
        )

