package com.ticketchief.payment.adapter.output;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.UUID;

@Repository
public class TransactionRepository {
    private final JdbcTemplate jdbc;

    public TransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Persist a transaction attempt. Returns the generated UUID as string.
     */
    public String insertTransaction(String correlationId, Long orderId, String userId, long amountCents, String status, String gatewayResponse) {
        String sql = "INSERT INTO transactions(correlation_id, order_id, user_id, amount_cents, status, gateway_response, created_at) VALUES (?, ?, ?, ?, ?, ?, now()) RETURNING id";
        return jdbc.queryForObject(sql, new Object[]{correlationId, orderId, userId != null ? UUID.fromString(userId) : null, amountCents, status, gatewayResponse}, String.class);
    }

    public int countAttempts(String correlationId) {
        String sql = "SELECT count(*) FROM transactions WHERE correlation_id = ?";
        Integer count = jdbc.queryForObject(sql, new Object[]{correlationId}, Integer.class);
        return count == null ? 0 : count;
    }
}
