package com.thom.ratelimiter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimiterTest {
    private RateLimiter rateLimiter;

    @Test
    void constructs() {
        rateLimiter = new RateLimiter(10, 10);
        assertNotNull(rateLimiter);
    }

    @Test
    void initsWithDefaults() {
        rateLimiter = new RateLimiter();
        assertEquals(10, rateLimiter.getMaxTokens());
        assertEquals(10, rateLimiter.getTokensPerSecond());
    }

    @Test
    void initsWithValues() {
        rateLimiter = new RateLimiter(5, 1);
        assertEquals(5, rateLimiter.getMaxTokens());
        assertEquals(1, rateLimiter.getTokensPerSecond());
    }

}
