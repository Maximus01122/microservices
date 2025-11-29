package com.ticketchief.orderservice.port.output;

import com.ticketchief.orderservice.domain.Order;

public interface InvoicePort {
    record InvoiceResult(String invoiceId, String url) {}
    InvoiceResult generateInvoice(Order order);
}
