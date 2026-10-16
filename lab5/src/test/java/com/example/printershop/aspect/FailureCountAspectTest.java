package com.example.printershop.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class FailureCountAspectTest {

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    private FailureCounter counter;
    private FailureCountAspect aspect;

    @BeforeEach
    void setUp() {
        counter = new FailureCounter();
        aspect = new FailureCountAspect(counter);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.toShortString()).thenReturn("OrderService.placeOrder(..)");
    }

    @Test
    @DisplayName("кожен виняток збільшує лічильник збоїв методу")
    void eachFailure_incrementsCounter() {
        aspect.countFailure(joinPoint, new IllegalStateException("boom"));
        aspect.countFailure(joinPoint, new RuntimeException("again"));

        assertThat(counter.get("OrderService.placeOrder(..)")).isEqualTo(2);
    }

    @Test
    @DisplayName("для методу без збоїв лічильник = 0")
    void noFailures_zero() {
        assertThat(counter.get("OrderService.other()")).isZero();
    }
}
