package ra.edu.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ra.edu.api.model.Inventory;
import reactor.core.publisher.Mono;


@Slf4j
@RestController
@RequestMapping("/api/inventory")
public class MockInventoryController {

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
