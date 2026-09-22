package ra.edu.api.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import ra.edu.api.model.Inventory;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class InventoryClientService {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;

    // Constructor injection (không dùng @Autowired — best practice)
    public InventoryClientService(WebClient inventoryWebClient,
                                   CircuitBreaker inventoryCircuitBreaker) {
        this.webClient = inventoryWebClient;
        this.circuitBreaker = inventoryCircuitBreaker;
    }

    public Mono<Inventory> checkInventory(String productId, boolean simulateFail) {
        log.info("[CB State: {}] Calling Inventory-Service for productId={}",
                circuitBreaker.getState(), productId);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/inventory/check")
                        .queryParam("productId", productId)
                        .queryParamIfPresent("fail",
                                simulateFail ? java.util.Optional.of(true) : java.util.Optional.empty())
                        .build())
                .retrieve()
                // Nếu upstream trả về HTTP 4xx/5xx → ném WebClientResponseException
                .onStatus(
                        status -> status.is5xxServerError() || status.is4xxClientError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new RuntimeException(
                                        "Inventory-Service error: HTTP " + response.statusCode() + " — " + body))
                )
                .bodyToMono(Inventory.class)

                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))

                // Xử lý TẤT CẢ lỗi (lỗi upstream + CB mở) → trả fallback
                .onErrorResume(ex -> {
                    log.warn("[FALLBACK] productId={}, CB State={}, reason={}",
                            productId, circuitBreaker.getState(), ex.getClass().getSimpleName());
                    return Mono.just(Inventory.fallback(productId, ex.getClass().getSimpleName()));
                });
    }
}
