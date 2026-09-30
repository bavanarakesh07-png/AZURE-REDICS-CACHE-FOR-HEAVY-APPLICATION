package com.example.demo.controller;

import com.example.demo.service.CacheStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
public class CacheStatsController {

    private final CacheStatsService cacheStatsService;

    public CacheStatsController(CacheStatsService cacheStatsService) {
        this.cacheStatsService = cacheStatsService;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {

        long hits = cacheStatsService.getCacheHits();
        long misses = cacheStatsService.getCacheMisses();
        long total = cacheStatsService.getTotalRequests();
        double hitRatio = cacheStatsService.getHitRatio();

        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("cacheHits", hits);
        stats.put("cacheMisses", misses);
        stats.put("totalRequests", total);
        stats.put(
                "hitRatioPercent",
                Math.round(hitRatio * 100.0) / 100.0
        );

        System.out.println();
        System.out.println("======================================");
        System.out.println("        CACHE STATISTICS");
        System.out.println("======================================");
        System.out.println("Cache Hits       : " + hits);
        System.out.println("Cache Misses     : " + misses);
        System.out.println("Total Requests   : " + total);
        System.out.println("Hit Ratio        : " + hitRatio + "%");
        System.out.println("======================================");
        System.out.println();

        return stats;
    }

    @PostMapping("/reset")
    public String resetStats() {

        cacheStatsService.resetStats();

        System.out.println();
        System.out.println("======================================");
        System.out.println("CACHE STATISTICS RESET");
        System.out.println("======================================");
        System.out.println();

        return "Cache statistics reset successfully";
    }
}