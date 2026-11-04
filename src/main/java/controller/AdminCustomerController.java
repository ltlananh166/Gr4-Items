package controller;

import model.Customer;
import service.CustomerAdminService;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Bộ điều hợp trình bày gọn nhẹ cho các thao tác quản lý khách hàng của quản trị viên. */
public class AdminCustomerController {
    private final CustomerAdminService customerAdminService;

    public AdminCustomerController(CustomerAdminService customerAdminService) {
        this.customerAdminService = Objects.requireNonNull(
                customerAdminService, "customerAdminService");
    }

    public Result<List<Customer>> list() {
        return execute(customerAdminService::list, "Customer list loaded.");
    }

    public Result<List<Customer>> search(String keyword) {
        return execute(() -> customerAdminService.search(keyword), "Search completed.");
    }

    public Result<Customer> create(String name, String email, String phone, String address,
                                   String tier, String password) {
        return execute(
                () -> customerAdminService.create(
                        name, email, phone, address, tier, password),
                "Customer created successfully.");
    }

    public Result<Customer> update(String customerId, String name, String email, String phone,
                                   String address, String tier, String newPassword) {
        return execute(
                () -> customerAdminService.update(customerId, name, email, phone,
                        address, tier, newPassword),
                "Customer updated successfully.");
    }

    public Result<Customer> setStatus(String customerId, String status) {
        return execute(() -> customerAdminService.setStatus(customerId, status),
                "Status updated successfully.");
    }

    public Result<Customer> delete(String customerId) {
        return execute(() -> customerAdminService.delete(customerId),
                "Customer deleted successfully.");
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
