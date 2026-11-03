package controller;

import model.Customer;
import repository.CustomerRepository;
import security.PasswordPolicy;
import security.PasswordSecurity;

import java.util.Map;
import java.util.Optional;

public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Optional<Customer> findCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return Optional.empty();
        }
        return customerRepository.findById(customerId.trim());
    }

    public Map<String, Object> register(Map<String, String> payload) {
        if (payload == null) {
            return Map.of("success", false, "message", "Registration details are missing.");
        }
        String email = payload.get("email");
        String name = payload.get("name");
        String phone = payload.get("phone");
        String address = payload.get("address");
        String password = payload.get("password");

        if (email == null || name == null || phone == null || password == null) {
            return Map.of("success", false, "message",
                    "Required fields are missing: name, email, phone number, and password.");
        }

        final String trimmedName = name.trim();
        final String trimmedEmail = email.trim();
        final String trimmedPhone = phone.trim();
        final String trimmedAddress = address != null ? address.trim() : "Default Address";

        if (trimmedName.isEmpty()) {
            return Map.of("success", false, "message", "Registration failed: name is required.");
        }

        if (trimmedEmail.isEmpty()) {
            return Map.of("success", false, "message", "Registration failed: email is required.");
        }

        if (trimmedPhone.isEmpty()) {
            return Map.of("success", false, "message", "Registration failed: phone number is required.");
        }

        Optional<String> passwordError = PasswordPolicy.validationError(password);
        if (passwordError.isPresent()) {
            return Map.of("success", false, "message", passwordError.get());
        }

        boolean exists = customerRepository.getAll().stream()
                .anyMatch(c -> c.getEmail().equalsIgnoreCase(trimmedEmail));

        if (exists) {
            return Map.of("success", false, "message", "Email already exists.");
        }

        String newId = customerRepository.generateNewCustomerId();

        Customer newCustomer = new Customer(
                newId, trimmedName, trimmedEmail, trimmedPhone, trimmedAddress);
        newCustomer.setPassword(PasswordSecurity.hash(password));
        customerRepository.save(newCustomer);

        return Map.of("success", true, "message", "Registration successful.", "customer", newCustomer);
    }

    public Optional<Customer> login(Map<String, String> payload) {
        if (payload == null) {
            return Optional.empty();
        }
        String email = payload.get("email");
        String password = payload.get("password");

        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            return Optional.empty();
        }

        Optional<Customer> candidate = customerRepository.findByEmail(email);
        if (candidate.isEmpty()) {
            return Optional.empty();
        }

        Customer customer = candidate.get();
        if (customer.getStatus() != model.enums.CustomerStatus.ACTIVE
                || !PasswordSecurity.matches(password, customer.getPassword())) {
            return Optional.empty();
        }

        if (PasswordSecurity.needsRehash(customer.getPassword())) {
            customer.setPassword(PasswordSecurity.hash(password));
            customerRepository.save(customer);
        }
        return Optional.of(customer);
    }

    /**
     * Cập nhật thông tin cá nhân: tên, SĐT, địa chỉ.
     * Không cho phép đổi email (tránh trùng) và không đổi mật khẩu ở đây.
     */
    public Map<String, Object> updateProfile(String customerId, Map<String, String> payload) {
        Customer c = customerRepository.findById(customerId).orElse(null);
        if (c == null) {
            return Map.of("success", false, "message", "Account not found.");
        }
        String name    = payload.get("name");
        String phone   = payload.get("phone");
        String address = payload.get("address");
        if (name    != null && !name.trim().isEmpty())    c.setName(name.trim());
        if (phone   != null && !phone.trim().isEmpty())   c.setPhone(phone.trim());
        if (address != null && !address.trim().isEmpty()) c.setAddress(address.trim());
        customerRepository.save(c);
        return Map.of("success", true, "message", "Profile updated successfully!", "customer", c);
    }

    /**
     * Đổi mật khẩu: yêu cầu nhập mật khẩu cũ đúng, mật khẩu mới >= 6 ký tự.
     */
    public Map<String, Object> changePassword(String customerId, String oldPassword, String newPassword) {
        Customer c = customerRepository.findById(customerId).orElse(null);
        if (c == null) {
            return Map.of("success", false, "message", "Account not found.");
        }
        if (!PasswordSecurity.matches(oldPassword, c.getPassword())) {
            return Map.of("success", false, "message", "The current password is incorrect.");
        }
        Optional<String> passwordError = PasswordPolicy.validationError(newPassword);
        if (passwordError.isPresent()) {
            return Map.of("success", false, "message", passwordError.get());
        }
        c.setPassword(PasswordSecurity.hash(newPassword));
        customerRepository.save(c);
        return Map.of("success", true, "message", "Password changed successfully!");
    }

    /**
     * Nạp tiền vào ví khách hàng.
     */
    public Map<String, Object> topUpWallet(String customerId, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            return Map.of("success", false, "message", "Top-up amount must be greater than zero.");
        }
        Customer c = customerRepository.findById(customerId).orElse(null);
        if (c == null) {
            return Map.of("success", false, "message", "Account not found.");
        }
        double updatedBalance = c.getWalletBalance() + amount;
        if (!Double.isFinite(updatedBalance)) {
            return Map.of("success", false, "message", "Top-up amount exceeds the allowed limit.");
        }
        c.setWalletBalance(updatedBalance);
        customerRepository.save(c);
        return Map.of("success", true, "message",
                String.format("Wallet topped up successfully! Current balance: %,.0f VND", c.getWalletBalance()),
                "customer", c);
    }
}

// Thành viên 3
