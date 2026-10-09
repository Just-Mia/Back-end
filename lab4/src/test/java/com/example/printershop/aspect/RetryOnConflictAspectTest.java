package com.example.printershop.aspect;

import com.example.printershop.domain.annotation.RetryOnConflict;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.ConcurrencyFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit-тест advice без Spring-контексту: ProceedingJoinPoint - mock. */
@ExtendWith(MockitoExtension.class)
class RetryOnConflictAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    private final RetryOnConflictAspect aspect = new RetryOnConflictAspect();
    private RetryOnConflict retry;

    @RetryOnConflict(maxAttempts = 3, backoffMs = 0)
    void annotatedMethod() {
    }

    @BeforeEach
    void setUp() throws Exception {
        retry = RetryOnConflictAspectTest.class.getDeclaredMethod("annotatedMethod").getAnnotation(RetryOnConflict.class);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.toShortString()).thenReturn("OrderService.placeOrder(..)");
    }

    @Test
    @DisplayName("успіх з першої спроби: proceed() рівно один раз")
    void success_proceedsOnce() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.retry(joinPoint, retry)).isEqualTo("ok");

        verify(joinPoint, times(1)).proceed();
    }

    @Test
    @DisplayName("тимчасовий збій: повторює і повертає результат")
    void transientFailure_retriesUntilSuccess() throws Throwable {
        when(joinPoint.proceed())
                .thenThrow(new ConcurrencyFailureException("deadlock"))
                .thenThrow(new ConcurrencyFailureException("deadlock"))
                .thenReturn("ok");

        assertThat(aspect.retry(joinPoint, retry)).isEqualTo("ok");

        verify(joinPoint, times(3)).proceed();
    }

    @Test
    @DisplayName("після вичерпання спроб кидає останній виняток")
    void exhausted_rethrows() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new ConcurrencyFailureException("deadlock"));

        assertThatThrownBy(() -> aspect.retry(joinPoint, retry)).isInstanceOf(ConcurrencyFailureException.class);

        verify(joinPoint, times(3)).proceed();
    }

    @Test
    @DisplayName("нетимчасовий виняток не повторюється")
    void nonTransient_notRetried() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("bug"));

        assertThatThrownBy(() -> aspect.retry(joinPoint, retry)).isInstanceOf(IllegalStateException.class);

        verify(joinPoint, times(1)).proceed();
    }
}
