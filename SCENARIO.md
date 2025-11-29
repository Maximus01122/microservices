# TicketChief: Full Integration Scenario

This document describes the complete end-to-end scenario supported by the integrated TicketChief platform.

The system allows users to register, create events, reserve seats, pay for orders, receive invoices/tickets via email, and verify tickets.

---

## Prerequisites

1.  Start the entire stack:
    ```bash
    docker-compose up --build
    ```
2.  Access the Frontend UI: **[http://localhost:3000](http://localhost:3000)**
3.  Other Tools:
    *   **RabbitMQ Management:** [http://localhost:15672](http://localhost:15672) (user/password)
    *   **MailHog (Email):** [http://localhost:8025](http://localhost:8025)
    *   **PostgreSQL:** Port 5432

---

## End-to-End Flow (Using Frontend UI)

The easiest way to test the integration is via the React Frontend.

1.  **Register User:**
    *   Go to the **Register** tab.
    *   Enter Name and Email.
    *   Click Register.
    *   *Backend Action:* Calls `POST /api/users`.

2.  **Login:**
    *   Switch to **Login** tab.
    *   Enter your Email.
    *   Click Login.
    *   *Backend Action:* Calls `POST /api/login`.

3.  **Create Event:**
    *   In the sidebar, enter "Rock Concert", Rows: 5, Cols: 10.
    *   Click "Create Event".
    *   *Backend Action:* Calls `POST /api/events`.

4.  **Reserve Seats:**
    *   Load the event using the ID (if not auto-loaded).
    *   Click on seats (Green = Available) to select them (Blue).
    *   Click "Reserve Selected Seats".
    *   *Backend Action:* Calls `POST /api/events/{id}/reserve` (User Service) -> `POST /api/orders` (Order Service creates cart).
    *   *Effect:* Seats turn Yellow (Reserved).

5.  **Pay & Finalize:**
    *   In the Cart section, click "Pay & Finalize".
    *   *Backend Action:* Calls `POST /api/orders/finalize/{id}`.
    *   *Process:*
        *   Order Service publishes `payment.requested`.
        *   Payment Service processes payment -> publishes `payment.processed`.
        *   Order Service receives `payment.processed`:
            *   Publishes `payment.validated` (for seat confirmation).
            *   Publishes `email.send` (for invoice).
        *   Event Service receives `payment.validated` -> Confirms seats (Persisted in DB).
        *   Notification Service receives `email.send` -> Sends email via MailHog.

6.  **Verify Result:**
    *   **Frontend:** The seats should turn Red (Confirmed) automatically after polling.
    *   **Email:** Check [http://localhost:8025](http://localhost:8025) for the invoice email.

---

## Manual API Walkthrough (cURL)

If you prefer to test individual services via terminal:

### 1. Create User (User Service)
```bash
curl -X POST http://localhost/api/users \
  -H "Content-Type: application/json" \
  -d '{"email": "demo@test.com", "name": "Demo User"}'
```
*Copy the `id` returned (USER_ID).*

### 2. Create Event (Event Service)
```bash
curl -X POST http://localhost/api/events \
  -H "Content-Type: application/json" \
  -d '{"name": "API Concert", "rows": 5, "cols": 5, "userId": "<USER_ID>"}'
```
*Copy the `id` returned (EVENT_ID).*

### 3. Reserve Seats (Event Service)
```bash
curl -X POST http://localhost/api/events/<EVENT_ID>/reserve \
  -H "Content-Type: application/json" \
  -d '{"userId": "<USER_ID>", "seats": ["A1", "A2"]}'
```

### 4. Create Order (Order Service)
```bash
curl -X POST http://localhost/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "<USER_ID>",
    "status": "IN_CART",
    "items": [
        { "eventId": "<EVENT_ID>", "seatId": "A1", "unitPriceCents": 1000 },
        { "eventId": "<EVENT_ID>", "seatId": "A2", "unitPriceCents": 1000 }
    ]
}'
```
*Copy `id` (ORDER_ID).*

### 5. Finalize & Pay (Order Service)
```bash
curl -X POST http://localhost/api/orders/finalize/<ORDER_ID>
```

### 6. Verify (Event Service)
```bash
curl http://localhost/api/events/<EVENT_ID>
```
*Seats A1 and A2 should now be "confirmed".*

