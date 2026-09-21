package ra.edu.api.model;

/**
 * DTO đại diện cho dữ liệu tồn kho nhận từ Inventory-Service.
 * Dùng Java Record để bất biến (immutable) và gọn gàng.
 *
 * @param productId  Mã sản phẩm cần kiểm tra
 * @param quantity   Số lượng tồn kho hiện tại
 * @param available  true = còn hàng, false = hết hàng
 * @param source     Cho biết dữ liệu đến từ "live" hay "fallback"
 */
public record Inventory(
        String productId,
        int quantity,
        boolean available,
        String source
) {
    /**
     * Factory method tạo response fallback khi Circuit Breaker mở hoặc lỗi.
     *
     * @param productId ID sản phẩm cần fallback
     * @param reason    Lý do fallback (ví dụ: tên exception)
     */
    public static Inventory fallback(String productId, String reason) {
        return new Inventory(productId, 0, false, "FALLBACK — " + reason);
    }
}
