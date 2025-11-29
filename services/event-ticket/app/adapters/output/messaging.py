import asyncio
import aio_pika
import orjson
from typing import Optional

# AMQP adapter to publish domain events and consume commands.
class Broker:
    def __init__(self, amqp_url: str, exchange_name: str = "ticketchief") -> None:
        self.amqp_url = amqp_url
        self.exchange_name = exchange_name
        self._connection: Optional[aio_pika.RobustConnection] = None
        self._channel: Optional[aio_pika.abc.AbstractChannel] = None
        self._exchange: Optional[aio_pika.abc.AbstractExchange] = None
        self._consumer_task: Optional[asyncio.Task] = None

    # Open connection/channel and declare the topic exchange.
    async def connect(self) -> None:
        self._connection = await aio_pika.connect_robust(self.amqp_url)
        self._channel = await self._connection.channel()
        self._exchange = await self._channel.declare_exchange(
            self.exchange_name, aio_pika.ExchangeType.TOPIC, durable=True
        )

        # Debug queue
        debug_queue = await self._channel.declare_queue("debug.events", durable=True)
        await debug_queue.bind(self._exchange, routing_key="#")

    # Close consumer task (if any) and connection.
    async def close(self) -> None:
        if self._consumer_task and not self._consumer_task.done():
            self._consumer_task.cancel()
        if self._connection:
            await self._connection.close()

    # Publish a JSON message using the given routing key.
    async def publish(self, routing_key: str, message: dict) -> None:
        assert self._exchange is not None
        body = orjson.dumps(message)
        await self._exchange.publish(
            aio_pika.Message(body=body, content_type="application/json"),
            routing_key=routing_key,
        )

    # Bind and consume the payment.validated command with the provided handler.
    async def consume_payment_validated(self, handler) -> None:
        assert self._channel is not None
        assert self._exchange is not None
        queue = await self._channel.declare_queue("event-ticket.payment", durable=True)
        await queue.bind(self._exchange, routing_key="payment.validated")

        async def _on_message(message: aio_pika.abc.AbstractIncomingMessage) -> None:
            async with message.process():
                try:
                    payload = orjson.loads(message.body)
                    await handler(payload)
                except Exception as exc:  # meaningful: do not ack on crash
                    print(f"[event-ticket] handler error: {exc}")
                    raise

        await queue.consume(_on_message, no_ack=False)

        # Prevent method exit to keep consumer alive
        self._consumer_task = asyncio.create_task(asyncio.Event().wait())

