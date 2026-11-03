package view;

import controller.VoucherController;
import model.Voucher;

import java.util.List;

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
        System.out.printf("%-15s | %-12s | %12s | %14s | %14s | %10s%n",
                "Code", "Type", "Value", "Max discount", "Minimum order", "Remaining uses");
        System.out.println("-".repeat(92));
        for (Voucher voucher : result.data()) {
            System.out.printf("%-15s | %-12s | %,12.0f | %,14.0f | %,14.0f | %,10d%n",
                    voucher.getCode(), voucher.getDiscountType(), voucher.getDiscountAmount(),
                    voucher.getMaxDiscountValue(), voucher.getMinOrderValue(),
                    voucher.getRemainingUses());
        }
    }
}

// Thành viên 3
