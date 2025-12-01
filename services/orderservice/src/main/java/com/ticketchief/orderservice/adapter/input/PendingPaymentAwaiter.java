package com.ticketchief.orderservice.adapter.input;

import com.ticketchief.common.events.PaymentProcessedEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class PendingPaymentAwaiter {
    private final Map<String, CompletableFuture<PaymentProcessedEvent>> map = new ConcurrentHashMap<>();

    public void complete(PaymentProcessedEvent event) {
        var f = map.remove(event.correlationId());
        if (f != null) {
            f.complete(event);
        }
    }

    public PaymentProcessedEvent await(String correlationId, Duration timeout) throws InterruptedException, ExecutionException, TimeoutException {
        var fut = new CompletableFuture<PaymentProcessedEvent>();
        map.put(correlationId, fut);
        try {
            return fut.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } finally {
            map.remove(correlationId);
        }
    }
}
