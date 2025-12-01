package com.ticketchief.payment.adapter.output;

import com.ticketchief.payment.domain.PaymentSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class PaymentSessionRepository {
    private final JdbcTemplate jdbc;

    public PaymentSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertSession(PaymentSession session) {
        String sql = "INSERT INTO payment_sessions(correlation_id, order_id, amount_cents, status, created_at) VALUES (?, ?, ?, ?, now())";
        jdbc.update(sql, session.getCorrelationId(), session.getOrderId(), session.getAmountCents(), session.getStatus());
    }

    public PaymentSession findByCorrelationId(String correlationId) {
        String sql = "SELECT correlation_id, order_id, amount_cents, status, created_at FROM payment_sessions WHERE correlation_id = ?";
        return jdbc.query(sql, new Object[]{correlationId}, rs -> {
            if (!rs.next()) return null;
            return mapRow(rs);
        });
    }

    public void updateStatus(String correlationId, String status) {
        String sql = "UPDATE payment_sessions SET status = ? WHERE correlation_id = ?";
        jdbc.update(sql, status, correlationId);
    }

    private PaymentSession mapRow(ResultSet rs) throws SQLException {
        return new PaymentSession(
                rs.getString("correlation_id"),
                rs.getObject("order_id") == null ? null : rs.getLong("order_id"),
                rs.getLong("amount_cents"),
                rs.getString("status"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null
        );
    }
}
