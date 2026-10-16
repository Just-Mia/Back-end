package com.example.printershop.service;

import com.example.printershop.configuration.AsyncConfig;
import com.example.printershop.domain.dto.InvoiceInfo;
import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.integration.PdfRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private final PdfRenderer pdfRenderer;

    @Async(AsyncConfig.EXECUTOR)
    public CompletableFuture<InvoiceInfo> generateInvoice(OrderResponse order) {
        byte[] pdf = pdfRenderer.render(order);
        return CompletableFuture.completedFuture(new InvoiceInfo("invoice-" + order.id() + ".pdf", pdf.length));
    }
}
