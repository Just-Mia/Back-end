package com.example.printershop.aspect;

import com.example.printershop.domain.annotation.RateLimited;
import com.example.printershop.exception.TooManyRequestsException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit-тест advice без Spring-контексту: ProceedingJoinPoint - mock. */
@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    private MutableClock clock;
    private RateLimitAspect aspect;
    private RateLimited limit;

    @RateLimited(maxCalls = 2, perSeconds = 10)
    void annotatedMethod() {
    }

    @BeforeEach
    void setUp() throws Exception {
        clock = new MutableClock();
        aspect = new RateLimitAspect(clock);
        limit = RateLimitAspectTest.class.getDeclaredMethod("annotatedMethod").getAnnotation(RateLimited.class);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.toShortString()).thenReturn("OrderService.placeOrder(..)");
        login("alice");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("під лімітом: proceed() викликається, результат повертається")
    void underLimit_callsProceedAndReturnsResult() throws Throwable {
        when(joinPoint.proceed()).thenReturn("result");

        assertThat(aspect.limit(joinPoint, limit)).isEqualTo("result");
        assertThat(aspect.limit(joinPoint, limit)).isEqualTo("result");

        verify(joinPoint, times(2)).proceed();
    }

    @Test
    @DisplayName("ліміт вичерпано: кидається виняток і proceed() більше НЕ викликається")
    void overLimit_throwsAndDoesNotProceed() throws Throwable {
        aspect.limit(joinPoint, limit);
        aspect.limit(joinPoint, limit);

        assertThatThrownBy(() -> aspect.limit(joinPoint, limit)).isInstanceOf(TooManyRequestsException.class);

        verify(joinPoint, times(2)).proceed();
    }

    @Test
    @DisplayName("після спливу вікна виклики знову дозволені")
    void afterWindow_allowsAgain() throws Throwable {
        aspect.limit(joinPoint, limit);
        aspect.limit(joinPoint, limit);
        clock.advanceSeconds(10);

        aspect.limit(joinPoint, limit);

        verify(joinPoint, times(3)).proceed();
    }

    @Test
    @DisplayName("ліміт рахується окремо для кожного користувача")
    void limitIsPerUser() throws Throwable {
        aspect.limit(joinPoint, limit);
        aspect.limit(joinPoint, limit);
        login("bob");

        aspect.limit(joinPoint, limit);

        verify(joinPoint, times(3)).proceed();
    }

    @Test
    @DisplayName("заблокований виклик не витрачає proceed()")
    void blockedCall_neverProceeds() throws Throwable {
        RateLimited zero = RateLimitAspectTest.class.getDeclaredMethod("zeroLimit").getAnnotation(RateLimited.class);

        assertThatThrownBy(() -> aspect.limit(joinPoint, zero)).isInstanceOf(TooManyRequestsException.class);

        verify(joinPoint, never()).proceed();
    }

    @RateLimited(maxCalls = 0, perSeconds = 10)
    void zeroLimit() {
    }

    private void login(String username) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(username, "n/a", "ROLE_USER");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
