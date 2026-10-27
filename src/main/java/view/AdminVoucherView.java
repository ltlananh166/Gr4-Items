package view;

import model.Voucher;
import repository.VoucherRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AdminVoucherView {
    private final VoucherRepository vouchers;
    private final ConsoleInput input;

    public AdminVoucherView(VoucherRepository vouchers, ConsoleInput input) {
        this.vouchers = Objects.requireNonNull(vouchers, "vouchers");
        this.input = Objects.requireNonNull(input, "input");
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n===== VOUCHER MANAGEMENT =====");
            System.out.println("1. List vouchers");
            System.out.println("2. Search by code");
            System.out.println("3. Add voucher");
            System.out.println("4. Set remaining uses (0 disables)");
            System.out.println("5. Delete voucher");
            System.out.println("0. Back");
            int choice = input.readIntMinMax("Choose an option", 0, 5,
                    "Choose a number from 0 to 5.");
            switch (choice) {
                case 1 -> displayVouchers(vouchers.getAll());
                case 2 -> searchVoucher();
                case 3 -> addVoucher();
                case 4 -> setRemainingUses();
                case 5 -> deleteVoucher();
                case 0 -> back = true;
                default -> throw new IllegalStateException("Validated voucher menu choice was out of range.");
            }
        }
    }

    private void searchVoucher() {
        String query = input.readStringRequired("Voucher code: ").toLowerCase(Locale.ROOT);
        displayVouchers(vouchers.getAll().stream()
                .filter(voucher -> voucher.getCode() != null
                        && voucher.getCode().toLowerCase(Locale.ROOT).contains(query))
                .toList());
    }

    private void addVoucher() {
        String code = input.readStringRequired("Code: ").toUpperCase(Locale.ROOT);
        if (vouchers.findByCode(code).isPresent()) {
            System.out.println("Voucher code already exists.");
            return;
        }
        String type = input.readStringRequired("Type (FIXED/PERCENTAGE): ")
                .toUpperCase(Locale.ROOT);
        BigDecimal value = readAmount("Discount value: ");
        BigDecimal maxDiscount = readAmount("Maximum discount: ");
        BigDecimal minimum = readAmount("Minimum order value: ");
        int uses = input.readIntMin("Remaining uses: ", 1, "Uses must be positive.");

        Voucher voucher = new Voucher(vouchers.generateNewVoucherId(), code, type,
                value, minimum, maxDiscount, null, null, model.enums.VoucherStatus.ACTIVE);
        voucher.setRemainingUses(uses);
        vouchers.save(voucher);
        System.out.println("Created voucher " + code + ".");
    }

    private void setRemainingUses() {
        String code = input.readStringRequired("Voucher code: ");
        Voucher voucher = vouchers.findByCode(code).orElse(null);
        if (voucher == null) {
            System.out.println("Voucher not found.");
            return;
        }
        int uses = input.readIntMin("New remaining uses (0 disables): ", 0,
                "Remaining uses cannot be negative.");
        voucher.setRemainingUses(uses);
        vouchers.update(voucher);
        System.out.println("Updated " + code + "; remaining uses: " + uses + ".");
    }

    private void deleteVoucher() {
        String voucherId = input.readStringRequired("Voucher ID: ");
        if (vouchers.deleteById(voucherId)) System.out.println("Voucher deleted.");
        else System.out.println("Voucher not found.");
    }

    private BigDecimal readAmount(String prompt) {
        while (true) {
            String value = input.readStringRequired(prompt);
            try {
                BigDecimal amount = new BigDecimal(value);
                if (amount.signum() >= 0) return amount;
            } catch (NumberFormatException ignored) {
                // Continue prompting until a valid amount is supplied.
            }
            System.out.println("Enter a non-negative amount.");
        }
    }

    private void displayVouchers(List<Voucher> items) {
        System.out.printf("Showing %d voucher(s).%n", items.size());
        System.out.printf("%-10s %-16s %-12s %12s %12s %12s %10s%n",
                "ID", "Code", "Type", "Value", "Min order", "Max off", "Uses");
        for (Voucher voucher : items) {
            System.out.printf("%-10s %-16s %-12s %,12.0f %,12.0f %,12.0f %10d%n",
                    voucher.getVoucherId(), voucher.getCode(), voucher.getDiscountType(),
                    voucher.getDiscountAmount().doubleValue(),
                    voucher.getMinOrderValue().doubleValue(),
                    voucher.getMaxDiscountValue().doubleValue(), voucher.getRemainingUses());
        }
    }
}
