# TicketChief Scenarios

The TicketChief platform combines five microservices (User, Event-Ticket, Order, Payment, Notification) plus a React frontend.

---

## Environment Checklist

1. Start every dependency:
   ```bash
   docker compose up --build
   ```
2. Frontend UI: [http://localhost:3000](http://localhost:3000) (proxying to each backend at `/api/**`)
3. Supporting tools:
   - RabbitMQ admin: [http://localhost:15672](http://localhost:15672) (user/user)
   - MailHog for outbound email: [http://localhost:8025](http://localhost:8025)
   - PostgreSQL: host `ticketchief-db`, port `5432`, db `ticketchief`, user `user`
4. REST base URL for curl samples: `http://localhost:3000/api`. This is the frontend dev server acting as a proxy. If you prefer to hit services directly, use: User `http://localhost:3002`, Event-Ticket `http://localhost:3001`, Order `http://localhost:8080`, Payment `http://localhost:8082`, Notification `http://localhost:8081`.

Environment variables already point services at the `ticketchief` RabbitMQ exchange and the shared queues defined in `docker-compose.yml`.

---

## Scenario 1 – Account Onboarding & Email Verification

**Supported behavior:** create a customer account, trigger an email verification workflow, and validate the token via REST.

1. Register the account (User Service):
   ```bash
   curl -X POST http://localhost:3000/api/users \
     -H "Content-Type: application/json" \
     -d '{"email":"demo+user@example.com","name":"Demo User","password":"Secret123!"}'
   ```
   - User service persists the profile, hashes the password, and publishes `user.email.verification.requested` (RabbitMQ exchange `ticketchief`, routing key `user.email.verification.requested`).
2. MailHog receives a message from Notification Service with a link such as `http://localhost:3000/email-verifications/<TOKEN>`.
3. Verify the account (User Service):
   ```bash
   curl -I http://localhost:3000/api/email-verifications/<TOKEN>
   ```
   - FastAPI marks `is_verified=true` and redirects to the frontend.
4. Create session / Login (optional REST call used by the frontend to fetch a session token):
   ```bash
   curl -X POST http://localhost:3000/api/sessions \
     -H "Content-Type: application/json" \
     -d '{"email":"demo+user@example.com","password":"Secret123!"}'
   ```

---

## Scenario 2 – Organizer Creates an Event

**Supported behavior:** a verified organizer configures a new venue layout.

1. Create the event (Event-Ticket Service):
   ```bash
   curl -X POST http://localhost:3000/api/events \
     -H "Content-Type: application/json" \
     -d '{
       "name":"Rock Night",
       "rows":5,
       "cols":8,
       "basePriceCents":15000,
       "userId":"<ORGANIZER_USER_ID>"
     }'
   ```
   - Seats plus pricing tiers are persisted in PostgreSQL (JSONB columns).
2. List events to confirm it appears:
   ```bash
   curl http://localhost:3000/api/events
   ```
   - Frontend also opens an SSE connection: `GET /api/events/<EVENT_ID>/updates` for live seat status streaming.

---

## Scenario 3 – Customer Reserves Seats & Builds a Cart

**Supported behavior:** hold seats temporarily, then mirror the hold inside the Order Service as cart items.

1. Reserve seats (Event-Ticket Service):
   ```bash
   curl -X POST http://localhost:3000/api/events/<EVENT_ID>/reservations \
     -H "Content-Type: application/json" \
     -d '{"userId":"<BUYER_ID>","seats":["A1","A2","A3"]}'
   ```
   - Response includes `reservationId` and ISO expiry.
   - Event Service publishes `seat.reserved` to RabbitMQ and pushes a `"reserved"` payload to any SSE listeners.
2. Create an order/cart (Order Service):
   ```bash
   curl -X POST http://localhost:3000/api/orders \
     -H "Content-Type: application/json" \
     -d '{
       "userId":"<BUYER_ID>",
       "status":"IN_CART",
       "items":[
         {"eventId":"<EVENT_ID>","seatId":"A1","unitPriceCents":15000,"reservationId":"<RES_ID>"},
         {"eventId":"<EVENT_ID>","seatId":"A2","unitPriceCents":13500,"reservationId":"<RES_ID>"},
         {"eventId":"<EVENT_ID>","seatId":"A3","unitPriceCents":12150,"reservationId":"<RES_ID>"}
       ]
     }'
   ```
   - Order Service stores each item with the reservation metadata so it can later confirm and release seats.

---

## Scenario 4 – Successful Purchase & Ticket Delivery

**Supported behavior:** collect card data, run the payment simulator, issue tickets with QR codes, and deliver an invoice email.

1. Initiate payment for order (Order Service):
   ```bash
   curl -X POST http://localhost:3000/api/orders/<ORDER_ID>/payments
   ```
   - Order Service emits `payment.requested` (routing key `payment.requested`).
2. Payment Service handles the request:
   - Creates a payment session, simulates processor behavior, and emits `payment.processed` followed by `payment.validated` when the simulator marks it successful (probability defined by `app.payment.simulator.success-rate`).
3. Event-Ticket Service consumes `payment.validated`:
   - Seats change from `reserved` to `confirmed`.
   - Tickets are persisted, QR codes generated (Python `qrcode` lib), and `ticket.created` is published.
4. Order Service consumes `ticket.created`:
   - Each cart item gains `ticketId` and `ticketQr`.
   - Once all tickets exist, the PDF invoice renderer embeds the QR images and Notification Service publishes `email.send`.
5. MailHog shows the invoice email with embedded QR codes; SSE clients receive `"confirmed"` seat updates automatically.

---

## Scenario 5 – Payment Failure with Card `666`

**Supported behavior:** decline the payment, cancel the order, and release held seats when the buyer uses the known-bad card number.

1. Run the standard reservation + cart steps (Scenario 3).
2. Submit failing card to the Payment Service:
   ```bash
   curl -X POST http://localhost:3000/api/payment-sessions/<CORRELATION_ID>/card-submissions \
     -H "Content-Type: application/json" \
     -d '{"cardNumber":"666","cardHolder":"Fraudulent User","cardCvv":"123"}'
   ```
   - The Payment Service contains a guard clause: if `cardNumber.equals("666")`, it immediately marks the attempt as `FAILED` and logs the decline.
3. Order Service reacts to the failed `payment.processed` event:
   - Publishes `seat.release-requested` (HTTP fallback is also available) so Event-Ticket can free the reservation.
   - `DELETE /api/reservations/<RES_ID>` is also invoked by the Order Service’s HTTP adapter for deterministic cleanup.
4. Event-Ticket Service broadcasts `"released"` payloads; the frontend sees the seats return to green, and no invoice email is sent.

---

These five scenarios cover every critical cross-service interaction: synchronous REST calls (user, events, orders, reservations, ticket validations) plus asynchronous RabbitMQ events (verification, payment, ticket issuance, notifications). Following them end-to-end demonstrates the platform’s happy path and a realistic negative path without touching internal code.***

