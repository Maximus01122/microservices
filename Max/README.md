[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/3wxgECtm)

# TicketChief — Microservices System

This repository contains three Spring Boot microservices and supporting infrastructure (RabbitMQ & MailHog) that simulate an order–payment–notification workflow.

---

## A) Technical Dependencies

### Required

- **Java 17+**
- **Maven 3.8+**
- **Docker** 20.10+ and **Docker Compose** v2
- **Git**
---

```bash
mvn clean package -DskipTests

docker compose build --no-cache

docker compose up -d

docker ps

# 🌐 6. Open key services:
# RabbitMQ UI: http://localhost:15672 (user/password)
# MailHog UI:  http://localhost:8025
# OrderService: http://localhost:8080
# PaymentService: http://localhost:8081
# NotificationService: http://localhost:8082
```

## Overview

| Service | Port | Description |
|----------|------|--------------|
| **OrderService** | 8080 | Handles orders, generates invoice PDFs, publishes payment requests |
| **PaymentService** | 8081 | Simulates payment processing, publishes payment processed events |
| **NotificationService** | 8082 | Listens for email events, fetches invoice PDFs, sends emails via SMTP |
| **RabbitMQ** | 5672 / 15672 | Message broker (UI on :15672) |
| **MailHog** | 1025 / 8025 | Development SMTP server (UI on :8025) |

---

