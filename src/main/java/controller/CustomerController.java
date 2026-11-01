package controller;

import exception.AuthenticationException;
import exception.FlashSaleRuleException;
import model.Customer;
import repository.CustomerRepository;
import service.CustomerService;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CustomerController {
    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private Customer currentUser;

    public CustomerController(CustomerService customerService, CustomerRepository customerRepository) {
        this.customerService = Objects.requireNonNull(customerService, "customerService");
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository");
    }

    public Optional<Customer> findCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return Optional.empty();
        }
        return customerRepository.findById(customerId.trim());
    }

    public Map<String, Object> register(Map<String, String> payload) {
        if (payload == null) {
            return result(false, "Thiếu thông tin đăng ký.");
        }

        String name = payload.get("name");
        String email = payload.get("email");
        String phone = payload.get("phone");
        String address = payload.get("address");
        String password = payload.get("password");
        if (name == null || email == null || phone == null || password == null) {
            return result(false,
                    "Thiếu thông tin bắt buộc: Họ tên, Email, Số điện thoại và Mật khẩu.");
        }

        try {
            Customer customer = customerService.register(
                    name, email, phone, address == null ? "" : address, password);
            return Map.of("success", true, "message", "Đăng ký thành công", "customer", customer);
        } catch (AuthenticationException exception) {
            return result(false, exception.getMessage());
        }
    }

    public Optional<Customer> login(Map<String, String> payload) {
        if (payload == null) {
            return Optional.empty();
        }
        String email = payload.get("email");
        String password = payload.get("password");
        try {
            currentUser = customerService.login(email, password);
            return Optional.of(currentUser);
        } catch (AuthenticationException exception) {
            currentUser = null;
            return Optional.empty();
        }
    }

    public void logout() {
        currentUser = null;
    }

    public Customer getCurrentUser() {
        return currentUser;
    }

    public Map<String, Object> updateProfile(String customerId, Map<String, String> payload) {
        if (payload == null) {
            return result(false, "Thiếu thông tin cập nhật.");
        }
        try {
            Customer customer = customerService.updateProfile(
                    customerId, payload.get("name"), payload.get("phone"),
                    payload.get("address"), "");
            if (currentUser != null && currentUser.getCustomerId().equals(customerId)) {
                currentUser = customer;
            }
            return Map.of("success", true, "message",
                    "Cập nhật thông tin thành công!", "customer", customer);
        } catch (AuthenticationException exception) {
            return result(false, exception.getMessage());
        }
    }

    public Map<String, Object> changePassword(
            String customerId, String oldPassword, String newPassword) {
        try {
            customerService.changePassword(customerId, oldPassword, newPassword);
            return result(true, "Đổi mật khẩu thành công!");
        } catch (AuthenticationException exception) {
            return result(false, exception.getMessage());
        }
    }

    public Map<String, Object> topUpWallet(String customerId, double amount) {
        try {
            customerService.topUpWallet(customerId, amount);
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Customer not found after wallet update: " + customerId));
            if (currentUser != null && currentUser.getCustomerId().equals(customerId)) {
                currentUser = customer;
            }
            return Map.of("success", true, "message",
                    String.format("Nạp tiền thành công! Số dư hiện tại: %,.0f VND",
                            customer.getWalletBalance()),
                    "customer", customer);
        } catch (FlashSaleRuleException exception) {
            return result(false, exception.getMessage());
        }
    }

    public double calculateTierDiscount(double amount) {
        return customerService.calculateTierDiscount(currentUser, amount);
    }

    public double applyVoucher(String code, double amount) {
        return customerService.applyVoucher(currentUser, code, amount);
    }

    public boolean checkFlashSaleLimit(int quantity) {
        try {
            customerService.validateFlashSalePurchase(quantity);
            return true;
        } catch (FlashSaleRuleException exception) {
            return false;
        }
    }

    private static Map<String, Object> result(boolean success, String message) {
        return Map.of("success", success, "message", message);
    }
}
