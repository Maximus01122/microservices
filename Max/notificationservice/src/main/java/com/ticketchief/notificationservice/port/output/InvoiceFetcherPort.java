package com.ticketchief.notificationservice.port.output;


public interface InvoiceFetcherPort {
    byte[] fetch(String invoiceUrlOrPath);
}
