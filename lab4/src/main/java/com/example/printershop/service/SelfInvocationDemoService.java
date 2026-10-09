package com.example.printershop.service;

import com.example.printershop.domain.annotation.RateLimited;
import org.springframework.stereotype.Service;


@Service
public class SelfInvocationDemoService {

    @RateLimited(maxCalls = 1, perSeconds = 60)
    public String limitedCall() {
        return "ok";
    }

    public String callViaThis() {
        return this.limitedCall();
    }
}
