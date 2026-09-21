package ra.edu.api.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Circuit Breaker theo cách PROGRAMMATIC (không dùng Annotation).
 *
 * Tại sao KHÔNG dùng @CircuitBreaker Annotation?
 * ─────────────────────────────────────────────────────────────────────
 * @CircuitBreaker là AOP-based: nó wrap method bằng một proxy đồng bộ.
 * Khi dùng với Reactive (Mono/Flux), AOP proxy có thể can thiệp vào
 * luồng scheduler, gây ra hiện tượng "blocking the event loop" hoặc
 * circuit breaker không hoạt động đúng.
 *
 * Giải pháp ĐÚNG cho WebFlux:
 *   .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
 * → Bọc trực tiếp trong Reactive pipeline, hoàn toàn non-blocking.
 *
 * Lưu ý về Metrics:
 * Spring Cloud Resilience4j auto-config tự động bind CB metrics sang
 * Micrometer khi có resilience4j-micrometer trên classpath.
 * KHÔNG cần gọi TaggedCircuitBreakerMetrics.bindTo() thủ công
 * (sẽ gây ra lỗi duplicate metric registration).
 */
@Configuration
public class AppCircuitBreakerConfig {

    /**
     * CircuitBreakerRegistry được Spring Cloud auto-configure dựa theo
     * cấu hình resilience4j.circuitbreaker.instances.* trong application.yaml.
     *
     * Ta chỉ cần lấy instance đã được đặt tên ra để inject vào Service/Controller.
     *
     * @param registry  Registry chứa tất cả CB instances (auto-configured)
     */
    @Bean
    public CircuitBreaker inventoryCircuitBreaker(CircuitBreakerRegistry registry) {
        // Lấy CircuitBreaker instance đã được cấu hình bằng tên trong YAML
        // Metrics tự động được bind bởi Spring Cloud Resilience4j auto-config
        // → Có thể thấy tại: GET /actuator/metrics/resilience4j.circuitbreaker.state
        return registry.circuitBreaker("inventoryCircuitBreaker");
    }
}

