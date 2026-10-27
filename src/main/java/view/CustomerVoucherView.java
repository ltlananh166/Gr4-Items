package view;

import controller.CustomerController;
import exception.FlashSaleRuleException;
import model.Voucher;
import repository.VoucherRepository;

import java.util.Objects;

public class CustomerVoucherView {
    private final CustomerController customers;
    private final VoucherRepository vouchers;
    private final ConsoleInput input;

    public CustomerVoucherView(CustomerController customers,
            VoucherRepository vouchers, ConsoleInput input) {
        this.customers = Objects.requireNonNull(customers, "customers");
        this.vouchers = Objects.requireNonNull(vouchers, "vouchers");
        this.input = Objects.requireNonNull(input, "input");
    }

    public void display() {
        System.out.println("\n--- AVAILABLE VOUCHERS ---");
        System.out.printf("%-16s %-12s %12s %12s %10s%n",
                "Code", "Type", "Discount", "Min order", "Uses");
        for (Voucher voucher : vouchers.getAll()) {
            if (voucher.isValid()) {
                System.out.printf("%-16s %-12s %,12.0f %,12.0f %10d%n",
                        voucher.getCode(), voucher.getDiscountType(),
                        voucher.getDiscountAmount().doubleValue(),
                        voucher.getMinOrderValue().doubleValue(),
                        voucher.getRemainingUses());
            }
        }
        String code = input.readLine("Voucher code (Enter to return): ");
        if (code.isEmpty()) return;

        double amount = readAmount();
        try {
            double finalAmount = customers.applyVoucher(code, amount);
            System.out.printf("Amount after discount: %,.0f VND. One use has been deducted.%n",
                    finalAmount);
        } catch (FlashSaleRuleException exception) {
            System.out.println("Voucher could not be applied: " + exception.getMessage());
        }
    }

    private double readAmount() {
        while (true) {
            String value = input.readStringRequired("Order amount (VND): ");
            try {
                double amount = Double.parseDouble(value);
                if (Double.isFinite(amount) && amount >= 0) return amount;
            } catch (NumberFormatException ignored) {
                // Continue prompting until a valid amount is supplied.
            }
            System.out.println("Enter a valid non-negative amount.");
        }
    }
}
