package com.example.printershop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.TimeZone;

@SpringBootTest
class ShopApplicationTests {

    static {
        // PostgreSQL відхиляє застарілу назву зони Europe/Kiev; main() ставить UTC, а тест його не викликає
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    void contextLoads() {
    }

}
