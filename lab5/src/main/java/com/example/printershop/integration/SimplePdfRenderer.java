package com.example.printershop.integration;

import com.example.printershop.domain.dto.OrderItemResponse;
import com.example.printershop.domain.dto.OrderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Component
public class SimplePdfRenderer implements PdfRenderer {

    private final long delayMs;

    public SimplePdfRenderer(@Value("${shop.simulated.pdf-delay-ms:1000}") long delayMs) {
        this.delayMs = delayMs;
    }

    @Override
    public byte[] render(OrderResponse order) {
        SimulatedDelay.sleep(delayMs);

        List<String> lines = new ArrayList<>();
        lines.add("Invoice for order #" + order.id());
        lines.add("Customer: " + order.customerUsername());
        for (OrderItemResponse item : order.items()) {
            lines.add(item.productName() + " x " + item.quantity());
        }
        return buildPdf(lines);
    }

    static byte[] buildPdf(List<String> lines) {
        StringBuilder content = new StringBuilder("BT /F1 12 Tf 50 780 Td 14 TL\n");
        for (String line : lines) {
            content.append('(').append(escape(line)).append(") Tj T*\n");
        }
        content.append("ET");

        String[] bodies = {
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R "
                        + "/Resources << /Font << /F1 5 0 R >> >> >>",
                "<< /Length " + content.length() + " >>\nstream\n" + content + "\nendstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
        };

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[bodies.length];
        for (int i = 0; i < bodies.length; i++) {
            offsets[i] = pdf.length();
            pdf.append(i + 1).append(" 0 obj\n").append(bodies[i]).append("\nendobj\n");
        }
        int xref = pdf.length();
        pdf.append("xref\n0 ").append(bodies.length + 1).append("\n0000000000 65535 f \n");
        for (int offset : offsets) {
            pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(bodies.length + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static String escape(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c < 32 || c > 126) {
                sb.append('?');
            } else if (c == '\\' || c == '(' || c == ')') {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
