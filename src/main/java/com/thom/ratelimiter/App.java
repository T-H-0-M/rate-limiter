package com.thom.ratelimiter;

import com.thom.ratelimiter.model.Request;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class App {
    private enum Mode {
        SINGLE_USER_BURST,
        MULTI_THREAD_SAME_USER
    }

    public static void main(String[] args) throws Exception {
        Mode mode = parseMode(args);

        int maxTokens = 10;
        int tokensPerSecond = 10;
        int durationSeconds = 8;
        int sleepMillisBetweenRequests = 25;
        int threads = 14;
        String userId = "user-1";

        RateLimiter limiter = new RateLimiter(maxTokens, tokensPerSecond);

        System.out.println("Rate limiter demo");
        System.out.println("mode=" + mode + " maxTokens=" + maxTokens + " tokensPerSecond=" + tokensPerSecond
                + " durationSeconds=" + durationSeconds);

        if (mode == Mode.SINGLE_USER_BURST) {
            runSingleUserBurst(limiter, userId, Duration.ofSeconds(durationSeconds), sleepMillisBetweenRequests);
        } else {
            runMultiThreadSameUser(limiter, userId, Duration.ofSeconds(durationSeconds), threads,
                    sleepMillisBetweenRequests);
        }
    }

    private static Mode parseMode(String[] args) {
        if (args.length == 0) {
            return Mode.SINGLE_USER_BURST;
        }

        String raw = args[0].toLowerCase(Locale.ROOT);
        return switch (raw) {
            case "multi", "concurrent", "threads" -> Mode.MULTI_THREAD_SAME_USER;
            default -> Mode.SINGLE_USER_BURST;
        };
    }

    private static void runSingleUserBurst(RateLimiter limiter, String userId, Duration duration,
            int sleepMillisBetweenRequests) throws InterruptedException {
        LongAdder accepted = new LongAdder();
        LongAdder rejected = new LongAdder();

        long startNanos = System.nanoTime();
        long deadlineNanos = startNanos + duration.toNanos();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(named("summary"));
        startSummaryLogger(scheduler, accepted, rejected, startNanos);

        long seq = 0;
        while (System.nanoTime() < deadlineNanos) {
            boolean ok = limiter.accept(new Request(userId));
            if (ok) {
                accepted.increment();
            } else {
                rejected.increment();
            }

            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            String verdict = ok ? "ACCEPT" : "REJECT";
            System.out.println("t=" + elapsedMs + "ms thread=" + Thread.currentThread().getName() + " user=" + userId
                    + " " + verdict + " a=" + accepted.sum() + " r=" + rejected.sum() + " req=" + (++seq));

            if (sleepMillisBetweenRequests > 0) {
                Thread.sleep(sleepMillisBetweenRequests);
            }
        }

        scheduler.shutdownNow();
        scheduler.awaitTermination(2, TimeUnit.SECONDS);

        System.out.println("Done. accepted=" + accepted.sum() + " rejected=" + rejected.sum());
    }

    private static void runMultiThreadSameUser(RateLimiter limiter, String userId, Duration duration, int threads,
            int sleepMillisBetweenRequests) throws InterruptedException {
        LongAdder accepted = new LongAdder();
        LongAdder rejected = new LongAdder();

        long startNanos = System.nanoTime();
        long deadlineNanos = startNanos + duration.toNanos();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(named("summary"));
        startSummaryLogger(scheduler, accepted, rejected, startNanos);

        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                long seq = 0;
                while (System.nanoTime() < deadlineNanos) {
                    boolean ok = limiter.accept(new Request(userId));
                    if (ok) {
                        accepted.increment();
                    } else {
                        rejected.increment();
                    }

                    long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
                    String verdict = ok ? "ACCEPT" : "REJECT";
                    System.out.println("t=" + elapsedMs + "ms thread=" + Thread.currentThread().getName() + " user="
                            + userId + " " + verdict + " a=" + accepted.sum() + " r=" + rejected.sum() + " req="
                            + (++seq));

                    if (sleepMillisBetweenRequests > 0) {
                        Thread.sleep(sleepMillisBetweenRequests);
                    }
                }
                return null;
            });
        }

        pool.shutdown();
        pool.awaitTermination(duration.plusSeconds(2).toMillis(), TimeUnit.MILLISECONDS);

        scheduler.shutdownNow();
        scheduler.awaitTermination(2, TimeUnit.SECONDS);

        System.out.println("Done. accepted=" + accepted.sum() + " rejected=" + rejected.sum());
    }

    private static void startSummaryLogger(ScheduledExecutorService scheduler, LongAdder accepted, LongAdder rejected,
            long startNanos) {
        final long[] last = new long[] { 0L, 0L };

        scheduler.scheduleAtFixedRate(() -> {
            long a = accepted.sum();
            long r = rejected.sum();
            long da = a - last[0];
            long dr = r - last[1];
            last[0] = a;
            last[1] = r;

            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            System.out.println("t=" + elapsedMs + "ms SUMMARY +a=" + da + " +r=" + dr + " totalA=" + a + " totalR="
                    + r);
        }, 1, 1, TimeUnit.SECONDS);
    }

    private static ThreadFactory named(String prefix) {
        return new ThreadFactory() {
            private long counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                counter++;
                Thread t = new Thread(r);
                t.setName(prefix + "-" + counter);
                t.setDaemon(true);
                return t;
            }
        };
    }
}
