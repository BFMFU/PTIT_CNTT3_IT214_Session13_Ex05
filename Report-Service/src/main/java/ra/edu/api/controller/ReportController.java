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

/**
 * Controller REACTIVE — tất cả phương thức trả về Mono<T>.
 *
 * Khác biệt với MVC Controller:
 *  - MVC:    ResponseEntity<?> checkInventory()       — thread bị chiếm giữ trong khi chờ
 *  - WebFlux: Mono<ResponseEntity<?>> checkInventory() — trả ngay Mono, thread được giải phóng
 *
 * Spring WebFlux sẽ subscribe() vào Mono và gửi response khi dữ liệu sẵn sàng.
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final InventoryClientService inventoryClientService;
    private final CircuitBreaker inventoryCircuitBreaker;

    /**
     * GET /api/reports/inventory?productId=P001
     * GET /api/reports/inventory?productId=P001&fail=true  ← kích hoạt lỗi để test CB
     *
     * @param productId     Mã sản phẩm cần kiểm tra tồn kho
     * @param simulateFail  Nếu true → upstream mock trả lỗi 500 (để test Circuit Breaker mở)
     * @return Mono<ResponseEntity<Inventory>> — non-blocking!
     */
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

    /**
     * GET /api/reports/circuit-status
     * Trả về trạng thái hiện tại của Circuit Breaker (tiện theo dõi khi test).
     */
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

