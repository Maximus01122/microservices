# SCENARIO — End-to-End Order → Payment → Email

This document explains **what scenario the system supports** and **exactly how to execute it** using copy-pasteable commands.

---

## A) What scenario is supported?

**End-to-end happy path (default)**

1. **OrderService (8080)** receives/creates an order.
3. It **publishes** a RabbitMQ message `payment.requested` to **`payments.exchange`**.
4. **PaymentService (8081)** consumes `payment.requested`, simulates payment, then **publishes** `payment.processed`.
5. **OrderService** consumes `payment.processed`, updates status to **PAID**, and **publishes** an email request (`email.send`) to **`notifications.exchange`**, including the invoice URL.
6. **NotificationService (8082)** fetches the PDF via **`http://orderservice:8080/files/invoices/{orderId}.pdf`**, then **sends an email** (captured by **MailHog** UI at `http://localhost:8025`).

**Failure path (simulated)**

- Payment may randomly fail (configurable), causing **`payment.processed`** with status `FAILED`.  
  Order status becomes **PAYMENT_FAILED** and no email/invoice is sent.

---

## B) How to execute the scenario
**Step 1 — create new order**

curl -sS -X POST http://localhost:8080/api/orders \
-H "Content-Type: application/json" \
-d '{
"status": "IN_CART",
"userId": 55,
"items": [
{ "eventId": 201, "seatId": 1001, "unitPriceCents": 4500 },
{ "eventId": 305, "seatId": 2021, "unitPriceCents": 6000 },
{ "eventId": 402, "seatId": 3022, "unitPriceCents": 7500 },
{ "eventId": 510, "seatId": 4010, "unitPriceCents": 9000 }
]
}'

`copy order_id from response: ORDER_ID=PASTE_YOUR_ID_HERE`


**Step 2 — Remove an Item from the Order**

curl -sS -X DELETE "http://localhost:8080/api/orders/remove/${ORDER_ID}" \
-H "Content-Type: application/json" \
-d '{
"id": 1,
"eventId": 201,
"seatId": 1001,
"unitPriceCents": 4500
}'


**Step 3 — Add Item to Order**

curl -sS -X PUT "http://localhost:8080/api/orders/add/${ORDER_ID}" \
-H "Content-Type: application/json" \
-d '{
"eventId": 701,
"seatId": 555,
"unitPriceCents": 2300
}'

**Step 4 — Finalize Order**

curl -sS -X POST "http://localhost:8080/api/orders/finalize/${ORDER_ID}"

**Step 5 — Verify the Results (happy path)**

open invoice http://localhost:8080/api/orders/${ORDER_ID}/invoice

check email http://localhost:8025





