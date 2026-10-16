package com.example.printershop.service;

import com.example.printershop.configuration.AsyncConfig;
import com.example.printershop.domain.dto.OrderResponse;
import com.example.printershop.integration.MailGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class EmailService {

    private final MailGateway mailGateway;

    @Async(AsyncConfig.EXECUTOR)
    public void sendOrderConfirmation(OrderResponse order) {
        mailGateway.send(order.customerUsername(),
                "Order #" + order.id() + " confirmed",
                "Thank you for your order #" + order.id() + " (" + order.items().size() + " item(s)).");
    }
}
