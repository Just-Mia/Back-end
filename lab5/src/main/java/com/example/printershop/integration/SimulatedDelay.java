package com.example.printershop.integration;



final class SimulatedDelay {

    private SimulatedDelay() {
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a slow call", e);
        }
    }
}
