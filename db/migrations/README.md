# PostgreSQL Migrations

This folder contains plain SQL migration files to create the necessary persistent schema for each service.

Files (apply in order):
- `1_users_service.sql` — users table, UUID support, timestamp trigger
- `2_event_ticket_service.sql` — events and tickets tables
- `3_orders_service.sql` — orders and order_items tables
- `4_payment_service.sql` — transactions table
- `5_notification_service.sql` — email_logs table

Notes:
- The migrations use `uuid-ossp` for `uuid_generate_v4()`; you can replace with `gen_random_uuid()` if you prefer `pgcrypto`.
- Each file is idempotent (uses `IF NOT EXISTS` where possible).

Applying migrations:

Set `DATABASE_URL` to a valid Postgres connection string, for example:

```bash
export DATABASE_URL="postgresql://postgres:password@localhost:5432/ticketchief"
```

Run all migrations in order:

```bash
for f in $(ls db/migrations/*.sql | sort); do
  echo "Applying $f";
  psql "$DATABASE_URL" -f "$f";
done
```

Alternatively run individual files via `psql "$DATABASE_URL" -f db/migrations/1_users_service.sql`.

Next steps (recommended):
- Update each service's configuration to point to a Postgres instance (connection URL, driver). For Java/Spring services, point `spring.datasource.*` to Postgres and add the JDBC driver dependency if not present.
- Consider adopting a migration tool (Flyway or Liquibase) for controlled versioning and automated application at service startup.
- For Python services, update `config/database.py` to use `psycopg2`/`asyncpg` and set the DSN from env var.
