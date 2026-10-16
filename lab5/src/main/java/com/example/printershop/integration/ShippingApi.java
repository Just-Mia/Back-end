package com.example.printershop.integration;

import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.domain.dto.ShippingQuote;



public interface ShippingApi {

    ShippingQuote quote(OrderResponse order);
}
