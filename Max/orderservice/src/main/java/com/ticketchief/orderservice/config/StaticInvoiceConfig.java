package com.ticketchief.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticInvoiceConfig implements WebMvcConfigurer {

    @Value("${app.invoice.storage-dir}")
    private String storageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // http://localhost:8080/files/invoices/1.pdf → file:{storageDir}/1.pdf
        registry.addResourceHandler("/files/invoices/**")
                .addResourceLocations("file:" + storageDir + "/")
                .setCachePeriod(0); // optional for dev
    }
}

