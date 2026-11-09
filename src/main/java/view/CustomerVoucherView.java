package view;

import java.util.List;

import controller.VoucherController;
import model.Voucher;

/** Danh sách phiếu giảm giá chỉ đọc dành cho khách hàng đã đăng nhập. */
public class CustomerVoucherView {
    private final VoucherController controller;

    public CustomerVoucherView(VoucherController controller) {
        this.controller = controller;
    }

    public void display() {
        VoucherController.Result<List<Voucher>> result = controller.listAvailable();
        if (!result.success() || result.data() == null) {
            System.out.println("Unable to load vouchers: " + result.message());
            return;
        }
        if (result.data().isEmpty()) {
            System.out.println("There are no vouchers with remaining uses.");
            return;
        }

        System.out.println("\n--- AVAILABLE VOUCHERS ---");
        System.out.printf("%-12s | %-10s | %10s | %12s | %12s | %8s%n",
                "Code", "Type", "Value", "Max discount", "Min order", "Uses left");
        System.out.println("-".repeat(78));
        for (Voucher voucher : result.data()) {
            System.out.printf("%-12s | %-10s | %,10.0f | %,12.0f | %,12.0f | %,8d%n",
                    voucher.getCode(), voucher.getDiscountType(), voucher.getDiscountAmount(),
                    voucher.getMaxDiscountValue(), voucher.getMinOrderValue(),
                    voucher.getRemainingUses());
        }
    }
}

