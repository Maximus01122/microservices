package com.ticketchief.payment.adapter.input;

import com.ticketchief.payment.domain.PaymentRequest;
import com.ticketchief.payment.domain.PaymentResult;
import com.ticketchief.payment.domain.PaymentSession;
import com.ticketchief.payment.adapter.output.PaymentSessionRepository;
import com.ticketchief.payment.application.PaymentService;
import com.ticketchief.payment.adapter.output.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment-sessions")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentSessionRepository sessionRepo;
    private final TransactionRepository txRepo;

    public PaymentController(PaymentService paymentService, PaymentSessionRepository sessionRepo, TransactionRepository txRepo) {
        this.paymentService = paymentService;
        this.sessionRepo = sessionRepo;
        this.txRepo = txRepo;
    }

    public static class AttemptRequest {
        public String cardNumber;
        public String cardCvv;
        public String cardHolder;
    }

    @PostMapping("/{correlationId}/attempt")
    public ResponseEntity<?> attempt(@PathVariable String correlationId, @RequestBody AttemptRequest body) {
        PaymentSession session = sessionRepo.findByCorrelationId(correlationId);
        if (session == null) return ResponseEntity.notFound().build();

        PaymentRequest request = new PaymentRequest(correlationId, session.getOrderId(), session.getAmountCents(), body.cardNumber, body.cardCvv, body.cardHolder);
        PaymentResult result = paymentService.attempt(request);
        int attempts = txRepo.countAttempts(correlationId);
        int remaining = Math.max(0, 3 - attempts);
        // re-fetch session to determine whether the session reached a final status
        PaymentSession sessionAfter = sessionRepo.findByCorrelationId(correlationId);
        boolean isFinal = sessionAfter != null && ("FAILED".equalsIgnoreCase(sessionAfter.getStatus()) || "SUCCESS".equalsIgnoreCase(sessionAfter.getStatus()));
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("status", result.getStatus().name());
        if (result.getReason() != null) resp.put("reason", result.getReason());
        resp.put("attemptsRemaining", remaining);
        resp.put("isFinal", isFinal);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{correlationId}")
    public ResponseEntity<?> getSession(@PathVariable String correlationId) {
        PaymentSession session = sessionRepo.findByCorrelationId(correlationId);
        if (session == null) return ResponseEntity.notFound().build();
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("correlationId", session.getCorrelationId());
        resp.put("orderId", session.getOrderId());
        resp.put("amountCents", session.getAmountCents());
        resp.put("status", session.getStatus());
        return ResponseEntity.ok(resp);
    }
}
