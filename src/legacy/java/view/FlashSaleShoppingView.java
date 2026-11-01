package view;

import controller.FlashSaleController;
import controller.OrderController;
import exception.OperationCancelledException;
import model.Customer;
import model.FlashSaleItem;

import java.util.List;
import java.util.Objects;

/**
 * Màn hình duyệt và đặt mua sản phẩm Flash Sale dành cho Khách hàng.
 * Tuân thủ đúng chuẩn kiến trúc MVC:
 * - Tầng View chỉ nhận dữ liệu từ bàn phím qua ConsoleInput.
 * - Gọi xuống FlashSaleController để duyệt sản phẩm và OrderController để đặt mua hàng.
 * - Gọi FlashSaleView và OrderView để hiển thị giao diện bảng và kết quả.
 */
public class FlashSaleShoppingView {
    // Quy định giới hạn Flash Sale: mỗi khách hàng chỉ được mua từ 1 đến 2 sản phẩm
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 2;

    private final FlashSaleController flashSaleController;
    private final OrderController orderController;
    private final FlashSaleView itemTable;
    private final OrderView orderView;
    private final ConsoleInput input;

    public FlashSaleShoppingView(FlashSaleController flashSaleController,
                                 OrderController orderController,
                                 FlashSaleView itemTable,
                                 OrderView orderView,
                                 ConsoleInput input) {
        this.flashSaleController = Objects.requireNonNull(flashSaleController, "flashSaleController must not be null");
        this.orderController = Objects.requireNonNull(orderController, "orderController must not be null");
        this.itemTable = Objects.requireNonNull(itemTable, "itemTable must not be null");
        this.orderView = Objects.requireNonNull(orderView, "orderView must not be null");
        this.input = Objects.requireNonNull(input, "input must not be null");
    }

    /**
     * Chức năng 1: Duyệt danh sách sản phẩm Flash Sale đang mở bán.
     * View gọi FlashSaleController lấy danh sách sản phẩm, sau đó dùng FlashSaleView để in ra bảng.
     */
    public void browse() {
        System.out.println("\n--- CURRENT FLASH SALE PRODUCTS ---");
        try {
            // Lấy tối đa 20 sản phẩm Flash Sale đang hoạt động từ Controller
            List<FlashSaleItem> items = flashSaleController.getFlashSaleItems(20);
            if (items == null || items.isEmpty()) {
                System.out.println("No Flash Sale products are currently available.");
                return;
            }
            // Gọi FlashSaleView để in bảng hiển thị ra màn hình Console
            itemTable.displayItems(items);
        } catch (Exception e) {
            System.out.println(">> Error loading Flash Sale items: " + e.getMessage());
        }
    }

    /**
     * Chức năng 2: Đặt mua sản phẩm Flash Sale.
     * Kiểm tra đăng nhập -> Chọn mã hàng -> Nhập số lượng (1-2) -> Áp voucher -> Bàn giao OrderController xử lý.
     */
    public boolean book(Customer customer) {
        // Bước 1: Kiểm tra xem người dùng đã đăng nhập chưa
        if (customer == null) {
            System.out.println("\n>> Please log in first before purchasing Flash Sale items!");
            return false;
        }

        // Bước 2: Hiển thị trước danh sách sản phẩm Flash Sale để khách chọn
        browse();

        System.out.println("\n--- PLACE FLASH SALE ORDER ---");
        System.out.println("(Type 'cancel' at any prompt to cancel order)");

        try {
            // Bước 3: Nhập mã sản phẩm Flash Sale
            String itemId = input.readStringRequired("Enter Flash Sale Item ID (e.g. ITEM001): ");

            // Bước 4: Nhập số lượng mua (Ràng buộc nghiêm ngặt chỉ từ 1 đến 2 sản phẩm)
            int quantity = input.readIntMinMax("Enter quantity", MIN_QUANTITY, MAX_QUANTITY,
                    "Flash Sale rule: Each customer can only purchase 1 to 2 items per event.");

            // Bước 5: Nhập mã giảm giá Voucher (nếu không có thì nhấn Enter bỏ trống)
            String voucherCode = input.readLine("Enter voucher code (leave blank if none): ");
            String normalizedVoucher = (voucherCode == null || voucherCode.isBlank()) ? null : voucherCode.trim();

            // Bước 6: Xác nhận đặt hàng
            String confirm = input.readLine("Confirm this order? (Y/N): ");
            if (!"Y".equalsIgnoreCase(confirm != null ? confirm.trim() : "")) {
                System.out.println(">> Order cancelled by user.");
                return false;
            }

            // =========================================================================
            // BƯỚC QUAN TRỌNG NHẤT: BÀN GIAO TOÀN BỘ CHO CONTROLLER XỬ LÝ
            // View chỉ gọi 1 dòng duy nhất sang OrderController.
            // Mọi logic trừ kho, khóa luồng chống oversell, tính VIP, trừ tiền ví... Controller sẽ lo!
            // =========================================================================
            boolean success = orderController.bookItem(
                    itemId,
                    quantity,
                    customer.getCustomerId(),
                    normalizedVoucher
            );

            // Bước 7: Gọi OrderView hiển thị kết quả thành công hay thất bại
            orderView.displayBookingResult(success);
            return success;

        } catch (OperationCancelledException exception) {
            // Bắt ngoại lệ khi người dùng gõ "cancel" tại bất kỳ bước nhập nào
            System.out.println(">> Order process cancelled. Returning to main menu.");
            return false;
        } catch (Exception exception) {
            // Nếu Controller ném ngoại lệ (hết hàng, hết tiền ví, lỗi voucher...), OrderView sẽ hiển thị
            orderView.displayBookingError(exception);
            return false;
        }
    }
}