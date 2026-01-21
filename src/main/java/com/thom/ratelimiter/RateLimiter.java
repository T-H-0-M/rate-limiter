package com.thom.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import com.thom.ratelimiter.model.BucketState;
import com.thom.ratelimiter.model.Request;

public class RateLimiter {
    private final ConcurrentHashMap<String, BucketState> buckets = new ConcurrentHashMap<>();
    private final int maxTokens;
    private final double tokensPerSecond;

    public RateLimiter() {
        this.maxTokens = 10;
        this.tokensPerSecond = 10;
    }

    public RateLimiter(int maxTokens, int tokensPerSecond) {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be greater than 0");
        }
        this.maxTokens = maxTokens;
        this.tokensPerSecond = tokensPerSecond;
    }

    public boolean accept(Request req) {
        boolean[] accept = new boolean[1];
        long now = System.nanoTime();

        buckets.compute(req.userId(), (key, state) -> {
            if (state == null) {
                accept[0] = true;
                return new BucketState(this.maxTokens - 1, now);
            }
            double secondsElapsed = (now - state.elapsedNanos()) / 1_000_000_000.0;
            double refillTokens = secondsElapsed * this.tokensPerSecond;
            double newTokens = Math.min(this.maxTokens, state.tokens() + refillTokens);
            if (newTokens >= 1) {
                accept[0] = true;
                return new BucketState(newTokens - 1, now);
            }
            accept[0] = false;
            return new BucketState(newTokens, now);
        });

        return accept[0];

    }

    public double getTokensPerSecond() {
        return tokensPerSecond;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

}
