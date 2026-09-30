package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class CacheStatsService {

    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    public void recordHit() {
        cacheHits.incrementAndGet();
    }

    public void recordMiss() {
        cacheMisses.incrementAndGet();
    }

    public long getCacheHits() {
        return cacheHits.get();
    }

    public long getCacheMisses() {
        return cacheMisses.get();
    }

    public long getTotalRequests() {
        return cacheHits.get() + cacheMisses.get();
    }

    public double getHitRatio() {

        long total = getTotalRequests();

        if (total == 0) {
            return 0.0;
        }

        return (cacheHits.get() * 100.0) / total;
    }

    public void resetStats() {
        cacheHits.set(0);
        cacheMisses.set(0);
    }
}
