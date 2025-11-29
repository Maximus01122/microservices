package com.ticketchief.notificationservice.adapter.output;

import com.ticketchief.notificationservice.port.output.InvoiceFetcherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class FileInvoiceFetcherAdapter implements InvoiceFetcherPort {

    private static final Logger log = LoggerFactory.getLogger(FileInvoiceFetcherAdapter.class);

    @Override
    public byte[] fetch(String invoiceUrlOrPath) {
        if (invoiceUrlOrPath == null || invoiceUrlOrPath.isBlank()) {
            throw new IllegalArgumentException("invoiceUrlOrPath must not be null or blank");
        }

        try {
            if (isHttpUrl(invoiceUrlOrPath)) {
                // HTTP(S) source
                Resource resource = toUrlResource(invoiceUrlOrPath);
                log.debug("Fetching invoice from URL: {}", invoiceUrlOrPath);
                if (!resource.exists()) {
                    throw new IllegalStateException("Invoice URL not reachable: " + invoiceUrlOrPath);
                }
                return resource.getContentAsByteArray();
            } else {
                // Local filesystem source
                Path path = Path.of(invoiceUrlOrPath).toAbsolutePath();
                log.debug("Fetching invoice from file: {}", path);
                if (!Files.exists(path)) {
                    throw new IllegalStateException("Invoice file not found: " + path);
                }
                return Files.readAllBytes(path);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load invoice from: " + invoiceUrlOrPath, e);
        }
    }

    private static boolean isHttpUrl(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static UrlResource toUrlResource(String url) {
        try {
            return new UrlResource(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid invoice URL: " + url, e);
        }
    }
}
