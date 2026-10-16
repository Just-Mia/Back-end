package com.example.printershop.integration;

import com.example.printershop.domain.dto.OrderResponse;



public interface PdfRenderer {

    byte[] render(OrderResponse order);
}
