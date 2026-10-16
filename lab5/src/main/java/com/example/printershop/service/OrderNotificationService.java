package com.example.printershop.service;

import com.example.printershop.domain.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNotificationService {

    private final EmailService emailService;



    public void notifyOrderPlaced(OrderResponse order) {
        try {
            emailService.sendOrderConfirmation(order);
        } catch (TaskRejectedException e) {
            log.warn("[Notify] executor is full - confirmation e-mail for order {} was skipped ({})",
                    order.id(), e.getMessage());
        }
    }
}
