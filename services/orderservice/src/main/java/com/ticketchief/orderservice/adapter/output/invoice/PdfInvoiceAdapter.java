package com.ticketchief.orderservice.adapter.output.invoice;

import com.ticketchief.orderservice.domain.Order;
import com.ticketchief.orderservice.port.output.InvoicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
public class PdfInvoiceAdapter implements InvoicePort {

    @Value("${app.invoice.storage-dir}")
    private String storageDir;

    @Value("${app.invoice.base-url}")
    private String baseUrl;

    @Override
    public InvoiceResult generateInvoice(Order order) {
        try {
            String invoiceId = String.valueOf(order.getId());
            byte[] pdf = PdfInvoiceRenderer.render(order);

            Path dir = Paths.get(storageDir);
            Files.createDirectories(dir);

            Path path = dir.resolve(invoiceId + ".pdf").toAbsolutePath();
            Files.write(path, pdf);

            String url = baseUrl + "/" + invoiceId + ".pdf";
            System.out.println("Invoice saved: " + path);
            System.out.println("Invoice URL: " + url);

            return new InvoiceResult(invoiceId, url);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate and store invoice", e);
        }


    }
}
