package view;

import controller.VoucherController;
import exception.OperationCancelledException;
import model.Voucher;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/** Giao diện dòng lệnh chỉ dùng để quản lý phiếu giảm giá. */
public class AdminVoucherView {
    private final VoucherController controller;
    private final ConsoleInput input;

    public AdminVoucherView(VoucherController controller) {
        this(controller, new ConsoleInput(new Scanner(System.in)));
    }

    public AdminVoucherView(VoucherController controller, ConsoleInput input) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.input = Objects.requireNonNull(input, "input");
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Select an option", 0)) {
                    case 1 -> showVouchers(controller.list());
                    case 2 -> search();
                    case 3 -> create();
                    case 4 -> update();
                    case 5 -> delete();
                    case 0 -> back = true;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (OperationCancelledException exception) {
                System.out.println("Operation cancelled.");
            }
        }
    }

    public void printMenu() {
        System.out.println("\n===== VOUCHER MANAGEMENT =====");
        System.out.println("1. List vouchers");
        System.out.println("2. Search vouchers");
        System.out.println("3. Add voucher");
        System.out.println("4. Update voucher");
        System.out.println("5. Delete voucher");
        System.out.println("0. Back");
    }

    public void search() {
        String keyword = input.readStringRequired("Enter voucher ID, code, or type");
        showVouchers(controller.search(keyword));
    }

    public void create() {
        String code = input.readStringRequired("Voucher code");
        String type = input.readStringRequired("Type PERCENTAGE/FIXED");
        int value = input.readIntRequired("Discount value");
        int maxDiscount = input.readIntRequired("Maximum discount");
        int minOrder = input.readIntRequired("Minimum order value");
        int uses = input.readIntRequired("Remaining uses");
        printAction(controller.create(code, type, value, maxDiscount, minOrder, uses));
    }

    public void update() {
        String key = input.readStringRequired("Voucher ID or code to update");
        System.out.println("Leave fields blank to keep current values.");
        String code = input.readLine("New code: ");
        String type = input.readLine("New type PERCENTAGE/FIXED: ");
        Integer value = readOptionalInt("New discount value: ");
        Integer maxDiscount = readOptionalInt("New maximum discount: ");
        Integer minOrder = readOptionalInt("New minimum order value: ");
        Integer uses = readOptionalInt("New remaining uses: ");
        printAction(controller.update(key, code, type, value, maxDiscount, minOrder, uses));
    }

    public void delete() {
        String key = input.readStringRequired("Voucher ID or code to delete");
        String confirmation = input.readLine("Enter Y to confirm deletion: ");
        if (!"Y".equalsIgnoreCase(confirmation)) {
            System.out.println("Deletion cancelled.");
            return;
        }
        printAction(controller.delete(key));
    }

    public Integer readOptionalInt(String prompt) {
        while (true) {
            String value = input.readLine(prompt);
            if (value.isBlank()) {
                return null;
            }
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException exception) {
                System.out.println("Enter an integer or leave blank.");
            }
        }
    }

    public void showVouchers(VoucherController.Result<List<Voucher>> result) {
        if (!result.success()) {
            System.out.println("Error: " + result.message());
            return;
        }
        List<Voucher> vouchers = result.data();
        if (vouchers == null || vouchers.isEmpty()) {
            System.out.println("No matching vouchers found.");
            return;
        }
        System.out.printf("%-9s | %-15s | %-10s | %10s | %12s | %12s | %8s%n",
                "ID", "ID", "Type", "Value", "Max discount", "Minimum order", "Remaining");
        System.out.println("-----------------------------------------------------------------------------------------------");
        for (Voucher voucher : vouchers) {
            System.out.printf("%-9s | %-15s | %-10s | %,10.0f | %,12.0f | %,12.0f | %,8d%n",
                    voucher.getVoucherId(), voucher.getCode(), voucher.getDiscountType(),
                    voucher.getDiscountAmount(), voucher.getMaxDiscountValue(),
                    voucher.getMinOrderValue(), voucher.getRemainingUses());
        }
    }

    public void printAction(VoucherController.Result<?> result) {
        System.out.println((result.success() ? "Success: " : "Error: ") + result.message());
    }
}

// Thành viên 3
