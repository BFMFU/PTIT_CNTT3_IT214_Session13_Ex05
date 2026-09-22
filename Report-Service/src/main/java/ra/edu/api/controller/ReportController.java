package ra.edu.api.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ra.edu.api.model.Inventory;
import ra.edu.api.service.InventoryClientService;
import reactor.core.publisher.Mono;


@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final InventoryClientService inventoryClientService;
    private final CircuitBreaker inventoryCircuitBreaker;

    @GetMapping("/inventory")
    public Mono<ResponseEntity<Inventory>> checkInventory(
            @RequestParam(defaultValue = "P001") String productId,
            @RequestParam(defaultValue = "false") boolean simulateFail) {

        log.info(">>> [ReportController] GET /api/reports/inventory?productId={}&fail={}",
                productId, simulateFail);

        // inventoryClientService.checkInventory() trả về Mono — KHÔNG block
        // .map() biến đổi Inventory thành ResponseEntity trong reactive chain
        return inventoryClientService.checkInventory(productId, simulateFail)
                .map(inventory -> {
                    // Nếu là fallback → trả 503, ngược lại 200 OK
                    if (inventory.source() != null && inventory.source().startsWith("FALLBACK")) {
                        log.warn("<<< [ReportController] Returning FALLBACK response");
                        return ResponseEntity.status(503).body(inventory);
                    }
                    log.info("<<< [ReportController] Returning LIVE data: {}", inventory);
                    return ResponseEntity.ok(inventory);
                });
    }

    @GetMapping("/circuit-status")
    public Mono<ResponseEntity<String>> getCircuitBreakerStatus() {
        String state = inventoryCircuitBreaker.getState().name();
        String metrics = String.format(
                "State: %s | FailureRate: %.1f%% | Calls: %d",
                state,
                inventoryCircuitBreaker.getMetrics().getFailureRate(),
                inventoryCircuitBreaker.getMetrics().getNumberOfBufferedCalls()
        );
        log.info("[CB Status] {}", metrics);
        return Mono.just(ResponseEntity.ok(metrics));
    }
}

