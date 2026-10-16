package com.example.printershop.aspect;

import com.example.printershop.exception.TooManyRequestsException;
import com.example.printershop.service.SelfInvocationDemoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;



@SpringJUnitConfig(SelfInvocationDemoTest.TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SelfInvocationDemoTest {

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        RateLimitAspect rateLimitAspect(Clock clock) {
            return new RateLimitAspect(clock);
        }

        @Bean
        SelfInvocationDemoService demoService() {
            return new SelfInvocationDemoService();
        }
    }

    @Autowired
    private SelfInvocationDemoService service;

    @Test
    @DisplayName("біну дійсно обгорнуто проксі")
    void beanIsProxy() {
        assertThat(AopUtils.isAopProxy(service)).isTrue();
    }

    @Test
    @DisplayName("зовнішній виклик: аспект спрацьовує (2-й виклик -> виняток)")
    void externalCall_triggersAspect() {
        assertThat(service.limitedCall()).isEqualTo("ok");

        assertThatThrownBy(() -> service.limitedCall()).isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("this.limitedCall(): аспект НЕ спрацьовує - жодних винятків і повідомлень (це задум вправи)")
    void selfInvocation_bypassesAspect() {
        for (int i = 0; i < 10; i++) {
            assertThatCode(() -> service.callViaThis()).doesNotThrowAnyException();
        }
    }
}
