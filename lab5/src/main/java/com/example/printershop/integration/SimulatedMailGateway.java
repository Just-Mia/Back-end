package com.example.printershop.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimulatedMailGateway implements MailGateway {

    private final long delayMs;

    public SimulatedMailGateway(@Value("${shop.simulated.mail-delay-ms:1000}") long delayMs) {
        this.delayMs = delayMs;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimulatedDelay.sleep(delayMs);
        log.info("[Mail] sent '{}' to {}", subject, to);
    }
}
