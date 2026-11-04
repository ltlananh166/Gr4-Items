package controller;

import model.Voucher;
import service.VoucherService;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Bộ điều hợp trình bày gọn nhẹ cho việc quản lý phiếu giảm giá. */
public class VoucherController {
    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = Objects.requireNonNull(voucherService, "voucherService");
    }

    public Result<List<Voucher>> list() {
        return execute(voucherService::list, "Voucher list loaded.");
    }

    public Result<List<Voucher>> listAvailable() {
        return execute(voucherService::listAvailable, "Available vouchers loaded.");
    }

    public Result<List<Voucher>> search(String keyword) {
        return execute(() -> voucherService.search(keyword), "Search completed.");
    }

    public Result<Voucher> create(String code, String type, int value, int maxDiscount,
                                  int minOrderAmount, int remainingUses) {
        return execute(() -> voucherService.create(code, type, value, maxDiscount,
                minOrderAmount, remainingUses), "Voucher created successfully.");
    }

    public Result<Voucher> update(String idOrCode, String code, String type, Integer value,
                                  Integer maxDiscount, Integer minOrderAmount,
                                  Integer remainingUses) {
        return execute(() -> voucherService.update(idOrCode, code, type, value,
                maxDiscount, minOrderAmount, remainingUses), "Voucher updated successfully.");
    }

    public Result<Voucher> delete(String idOrCode) {
        return execute(() -> voucherService.delete(idOrCode), "Voucher deleted successfully.");
    }

    public static <T> Result<T> execute(Supplier<T> action, String successMessage) {
        try {
            return new Result<>(true, successMessage, action.get());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return new Result<>(false, exception.getMessage(), null);
        }
    }

    public record Result<T>(boolean success, String message, T data) {
    }
}

// Thành viên 3
