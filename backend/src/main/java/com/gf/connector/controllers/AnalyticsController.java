package com.gf.connector.controllers;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final MeterRegistry meterRegistry;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> overview() {
        double httpCount = meterRegistry.counter("http_server_requests_total").count();
        Double jvmMemUsed = meterRegistry.find("jvm.memory.used").gauge() != null
                ? meterRegistry.find("jvm.memory.used").gauge().value()
                : 0d;
        return ResponseEntity.ok(Map.of(
                "requests", httpCount,
                "jvmMemoryUsed", jvmMemUsed
        ));
    }

    @GetMapping("/live")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> live() {
        Double liveUsers = meterRegistry.find("app_live_users").gauge() != null
                ? meterRegistry.find("app_live_users").gauge().value()
                : 0d;
        return ResponseEntity.ok(Map.of("liveUsers", liveUsers));
    }

    @GetMapping("/sla")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> sla() {
        Timer timer = meterRegistry.find("http.server.requests").timer();
        if (timer == null) {
            return ResponseEntity.ok(Map.of(
                    "p95_ms", 0,
                    "count", 0
            ));
        }
        Double p95 = timer.takeSnapshot().percentileValues().length > 0
                ? timer.takeSnapshot().percentileValues()[Math.min( (int)Math.floor(0.95 * (timer.takeSnapshot().percentileValues().length - 1)), timer.takeSnapshot().percentileValues().length - 1)].value(TimeUnit.MILLISECONDS)
                : 0d;
        long count = timer.count();
        return ResponseEntity.ok(Map.of(
                "p95_ms", p95.longValue(),
                "count", count
        ));
    }

    @GetMapping("/thresholds")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> thresholds() {
        // Placeholder para thresholds configurables
        return ResponseEntity.ok(Map.of(
                "warnings", 0,
                "alerts", 0,
                "latency_threshold_ms", 500
        ));
    }
}


