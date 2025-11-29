package com.ticketchief.orderservice.adapter.output.invoice;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ticketchief.orderservice.domain.CartItem;
import com.ticketchief.orderservice.domain.Order;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PdfInvoiceRenderer {

    private static final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.CANADA);

    private PdfInvoiceRenderer() {}

    public static byte[] render(Order order) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Header
            var title = new Paragraph("Invoice - Ticketchief", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20));
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Order ID: " + order.getId()));
            doc.add(new Paragraph("User ID: " + order.getUserId()));
            doc.add(new Paragraph("Date: " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)));
            doc.add(new Paragraph(" "));

            // Items table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2.5f, 2.5f, 2.0f, 2.0f});
            table.addCell("Event ID");
            table.addCell("Seat ID");
            table.addCell("Price");
            table.addCell("Quantity");

            order.getItems().forEach(it -> {
                table.addCell(String.valueOf(it.eventId()));
                table.addCell(String.valueOf(it.seatId()));
                table.addCell(currency.format(it.unitPriceCents() / 100.0));
                table.addCell("1");
            });
            doc.add(table);

            doc.add(new Paragraph(" "));
            long total = order.getItems().stream().mapToLong(CartItem::unitPriceCents).sum();
            String totalStr = currency.format(total / 100.0);
            var totalP = new Paragraph("Total: " + totalStr, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            totalP.setAlignment(Element.ALIGN_RIGHT);
            doc.add(totalP);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render invoice PDF", e);
        }
    }
}

