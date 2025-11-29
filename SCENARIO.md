# Scenario

This shows how to create an event, reserve seats, pay, get ticket QR(s), and verify a ticket.

Before you start
- Run: `docker compose up --build`
- RabbitMQ UI: `http://localhost:15672` (guest/guest)
- Swagger UIs: `http://localhost:3001/docs` (event-ticket), `http://localhost:3002/docs` (user)

1) Create an event (REST)
- Send POST to http://localhost:3001/events with a name, rows, and cols. Copy the `id`.

2) Register a user (REST)
- Send POST to http://localhost:3002/users. Copy the `id` (this is your `userId`).

3) Reserve seats (REST)
- Send POST to http://localhost:3001/events/{id}/reserve with:
  {
    "userId": "<user-id>",
    "seats": ["A1", "A2"]
  }
- Rules: same row, contiguous, max 8. Holds auto-expire after 10 minutes if not paid.

4) Prepare to see results (RabbitMQ)
- Create queue `debug.events` (Durable). Bind it to exchange `ticketchief` with `ticket.created` and `seat.confirmed`.

5) Pay (RabbitMQ)
- In Exchanges → `ticketchief` → Publish message.
- Routing key: `payment.validated`
- Payload:
  ```json
  {
    "orderId": "demo-1",
    "eventId": "<event-id>",
    "seats": ["A1", "A2"],
    "userId": "<same-user-id-who-reserved>"
  }
  ```
- Only seats reserved by that user will be confirmed.

1) Get the ticket QR(s)
- In `debug.events`, click “Get messages”. You should see one `ticket.created` per seat. Each has `ticketId`, `eventId`, `seat`, and `qr` (data URL). Paste the `qr` in your browser to see the PNG.

1) Verify a ticket (REST)
- Send POST to http://localhost:3001/tickets/verify with:
  {
    "ticketId": "<from ticket.created>",
    "eventId": "<event-id>"
  }
- You should get `valid: true` for confirmed seats.

Notes
- Holds: set HOLD_MINUTES env to change the 10-minute default.