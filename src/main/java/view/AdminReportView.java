package view;

import java.util.Objects;

import controller.AdminReportController;
import model.enums.CustTier;
import model.enums.OrderStatus;
import service.AdminReportService;

/** Hiển thị chỉ đọc các số liệu phân tích của quản trị viên trên giao diện dòng lệnh. */
public class AdminReportView {
    private final AdminReportController controller;

    public AdminReportView(AdminReportController controller) {
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    public void display() {
        AdminReportController.Result result = controller.getReport();
        if (!result.success() || result.report() == null) {
            System.out.println("Error: " + result.message());
            return;
        }
        AdminReportService.Report report = result.report();
        System.out.println("\n===== ADMIN REPORT =====");
        System.out.printf("Revenue from SUCCESS orders: %,.0f VND%n", report.totalRevenue());
        System.out.printf("Successful orders: %,d | Other orders: %,d%n",
                report.successfulOrders(), report.otherOrders());

        System.out.println("\n--- ORDERS BY STATUS ---");
        for (OrderStatus status : OrderStatus.values()) {
            System.out.printf("%-18s: %,d%n", status,
                    report.ordersByStatus().getOrDefault(status, 0L));
        }

        System.out.println("\n--- CUSTOMERS BY TIER ---");
        for (CustTier tier : CustTier.values()) {
            System.out.printf("%-18s: %,d%n", tier,
                    report.customersByTier().getOrDefault(tier, 0L));
        }

        System.out.println("\n--- VOUCHER STATISTICS ---");
        if (report.vouchers().isEmpty()) {
            System.out.println("No vouchers found.");
            return;
        }
        System.out.printf("%-14s | %-10s | %10s | %12s | %10s | %12s%n",
                "ID", "Type", "Value", "Max discount", "Remaining", "SUCCESS orders");
        System.out.println("--------------------------------------------------------------------------------");
        for (AdminReportService.VoucherMetric voucher : report.vouchers()) {
            System.out.printf("%-14s | %-10s | %,10.0f | %,12.0f | %,10d | %,12d%n",
                    voucher.code(), voucher.type(), voucher.value(), voucher.maxDiscount(),
                    voucher.remainingUses(), voucher.successfulUses());
        }
    }
}

