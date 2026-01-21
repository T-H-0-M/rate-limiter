package com.thom.ratelimiter.model;

public record BucketState(double tokens, long elapsedNanos) {
}
