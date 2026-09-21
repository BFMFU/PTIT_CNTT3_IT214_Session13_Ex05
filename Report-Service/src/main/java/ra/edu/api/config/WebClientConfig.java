package ra.edu.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Cấu hình WebClient — HTTP client non-blocking của Spring WebFlux.
 *
 * WebClient hoạt động hoàn toàn bất đồng bộ trên Reactor Netty,
 * KHÔNG chiếm giữ thread trong khi chờ response (khác với RestTemplate).
 */
@Configuration
public class WebClientConfig {

    /**
     * Base URL của Inventory-Service, đọc từ application.yaml:
     *   inventory.service.base-url: http://localhost:8080
     */
    @Value("${inventory.service.base-url}")
    private String inventoryServiceBaseUrl;

    /**
     * Bean WebClient được cấu hình sẵn baseUrl trỏ tới Inventory-Service.
     * Inject vào InventoryClientService để thực hiện HTTP call.
     */
    @Bean
    public WebClient inventoryWebClient() {
        return WebClient.builder()
                .baseUrl(inventoryServiceBaseUrl)
                // Có thể thêm default headers, codecs, filters ở đây
                .build();
    }
}
