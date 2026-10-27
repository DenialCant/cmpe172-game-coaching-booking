package com.cmpe172.game_coaching_booking.observability;

import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ManagementController {
    private static final Logger log = LoggerFactory.getLogger(ManagementController.class);
    private final DataSource dataSource;
    private final SchedulerMetrics metrics;

    public ManagementController(DataSource dataSource, SchedulerMetrics metrics) {
        this.dataSource = dataSource;
        this.metrics = metrics;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return ResponseEntity.ok(Map.of("status", "UP", "database", "UP"));
            }
            log.error("Database health check failed reason=invalid-connection");
        } catch (Exception exception) {
            log.error("Database health check failed reason={}", exception.getClass().getSimpleName());
        }
        return ResponseEntity.status(503).body(Map.of("status", "DOWN", "database", "DOWN"));
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() { return metrics.snapshot(); }
}
