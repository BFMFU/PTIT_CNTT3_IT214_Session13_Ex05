package ra.edu.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ra.edu.api.model.Inventory;
import reactor.core.publisher.Mono;

/**
 * ════════════════════════════════════════════════════════════════
 *  MOCK INVENTORY-SERVICE (chỉ dùng cho mục đích học tập)
 * ════════════════════════════════════════════════════════════════
 *
 * Trong thực tế, đây là một microservice riêng chạy trên port 8081.
 * Ở đây ta giả lập nó ngay trong cùng ứng dụng để demo không cần
 * deploy thêm service.
 *
 * API giả lập:
 *   GET /api/inventory/check?productId=P001         → 200 OK (bình thường)
 *   GET /api/inventory/check?productId=P001&fail=true → 500 ERROR (kích hoạt lỗi)
 *
 * Kịch bản test Circuit Breaker:
 *   1. Gọi ?fail=true 4 lần liên tiếp → CB ghi nhận lỗi
 *   2. Lần thứ 5 → failure rate >= 60% → CB chuyển sang OPEN
 *   3. Gọi tiếp → CB OPEN, không gọi upstream, trả fallback ngay
 *   4. Sau 10 giây → CB chuyển HALF-OPEN, thử 2 request
 *   5. Nếu thành công → CB CLOSED (bình thường trở lại)
 */
@Slf4j
@RestController
@RequestMapping("/api/inventory")
public class MockInventoryController {

    /**
     * Endpoint giả lập kiểm tra tồn kho.
     *
     * @param productId  Mã sản phẩm
     * @param fail       Nếu true → giả lập lỗi 500 để kích hoạt Circuit Breaker mở
     */
    @GetMapping("/check")
    public Mono<ResponseEntity<Inventory>> checkInventory(
            @RequestParam(defaultValue = "P001") String productId,
            @RequestParam(defaultValue = "false") boolean fail) {

        if (fail) {
            log.error("[MOCK INVENTORY] Simulating ERROR for productId={}", productId);
            // Trả 500 → WebClient sẽ ném exception → CB ghi nhận failure
            return Mono.just(
                    ResponseEntity.internalServerError()
                            .<Inventory>build()
            );
        }

        // Tạo dữ liệu giả lập dựa theo productId
        Inventory inventory = switch (productId) {
            case "P001" -> new Inventory("P001", 150, true, "live");
            case "P002" -> new Inventory("P002", 0, false, "live");
            case "P003" -> new Inventory("P003", 42, true, "live");
            default -> new Inventory(productId, 99, true, "live");
        };

        log.info("[MOCK INVENTORY] Returning data for productId={}: {}", productId, inventory);
        return Mono.just(ResponseEntity.ok(inventory));
    }
}
