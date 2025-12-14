# TicketChief

TicketChief is a microservices-based ticketing platform for event management and sales.

## Architecture

The project consists of the following microservices:

*   **Frontend (React):** User interface for browsing events, booking seats, and managing orders.
*   **User Service (Python/FastAPI):** Manages user registration and profiles.
*   **Event-Ticket Service (Python/FastAPI):** Manages events, seat maps, reservations, and ticket generation.
*   **Order Service (Java/Spring Boot):** Handles shopping carts, order finalization, and invoice generation.
*   **Payment Service (Java/Spring Boot):** Simulates payment processing.
*   **Notification Service (Java/Spring Boot):** Sends emails (invoices/tickets).

**Infrastructure:**
*   **RabbitMQ:** Message broker for asynchronous communication (Payment events, Email requests).
*   **PostgreSQL:** Persistent database (one per service for full isolation).
*   **MailHog:** SMTP testing tool to capture emails.
*   **Nginx:** Reverse proxy serving the frontend and routing API requests.

## Prerequisites

*   Docker & Docker Compose

## Running the Application

1.  **Build and Start:**
    ```bash
    docker-compose up --build
    ```

2.  **Access the UI:**
    Open [http://localhost:3000](http://localhost:3000) in your browser.

## Development Links

*   **Frontend:** [http://localhost:3000](http://localhost:3000)
*   **RabbitMQ Management:** [http://localhost:15672](http://localhost:15672) (user/password)
*   **MailHog:** [http://localhost:8025](http://localhost:8025)
*   **API Docs:**
    *   User: [http://localhost:3002/docs](http://localhost:3002/docs)
    *   Event: [http://localhost:3001/docs](http://localhost:3001/docs)

## Running Tests

### Python Services (User, Event-Ticket)

```bash
# User Service
cd services/user
pip install -r requirements.txt
pytest -v

# Event-Ticket Service
cd services/event-ticket
pip install -r requirements.txt
pytest -v
```

### Java Services (Order, Payment)

```bash
# First, install parent POM and common module
cd services/orderservice
./mvnw.cmd install -f ../../pom.xml -DskipTests -N
./mvnw.cmd install -f ../common/pom.xml -DskipTests

# Order Service unit tests
cd services/orderservice
./mvnw.cmd test -Dtest=OrderTest

# Payment Service unit tests
cd services/payment
./mvnw.cmd test -Dtest=PaymentServiceTest
```

**Note:** Integration tests require RabbitMQ to be running.

## Usage Scenario

See [SCENARIO.md](SCENARIO.md) for a detailed walkthrough of the end-to-end flow.
See [TicketChief-Scenarios.postman_collection.json](TicketChief-Scenarios.postman_collection.json) for the Postman collection.