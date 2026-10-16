package com.example.printershop.service;

import com.example.printershop.domain.dto.OrderResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderNotificationServiceTest {

    @Mock
    private EmailService emailService;
    @InjectMocks
    private OrderNotificationService service;

    private final OrderResponse order = new OrderResponse(7, "customer", OffsetDateTime.now(), List.of());

    @Test
    @DisplayName("передає замовлення в EmailService")
    void delegatesToEmailService() {
        service.notifyOrderPlaced(order);

        verify(emailService).sendOrderConfirmation(order);
    }

    @Test
    @DisplayName("TaskRejectedException з EmailService не прориває запит")
    void rejectionIsSwallowed() {
        doThrow(new TaskRejectedException("full")).when(emailService).sendOrderConfirmation(any());

        assertThatCode(() -> service.notifyOrderPlaced(order)).doesNotThrowAnyException();
    }
}
