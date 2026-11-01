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
        return Optional.ofNullable(customerRepository.findById(customerId.trim()));
    }

    public Map<String, Object> register(Map<String, String> payload) {
        if (payload == null) {
            return Map.of("success", false, "message", "Thiếu thông tin đăng ký.");
        }
        String email = payload.get("email");
        String name = payload.get("name");
        String phone = payload.get("phone");
        String address = payload.get("address");
        String avatarUrl = payload.get("avatarUrl");
        String password = payload.get("password");

        if (email == null || name == null || phone == null || password == null) {
            return Map.of("success", false, "message",
                    "Thiếu thông tin bắt buộc: Họ tên, Email, Số điện thoại và Mật khẩu");
        }

        String trimmedName = name.trim();
        String trimmedEmail = email.trim();
        String trimmedPhone = phone.trim();
        String trimmedAddress = address != null ? address.trim() : "Default Address";

        if (trimmedName.isEmpty()) {
            return Map.of("success", false, "message", "Đăng ký thất bại: Họ tên không được để trống");
        }

        if (trimmedEmail.isEmpty()) {
            return Map.of("success", false, "message", "Đăng ký thất bại: Email không được để trống");
        }

        if (trimmedPhone.isEmpty()) {
            return Map.of("success", false, "message", "Đăng ký thất bại: Số điện thoại không được để trống");
        }

        Optional<String> passwordError = PasswordPolicy.validationError(password);
        if (passwordError.isPresent()) {
            return Map.of("success", false, "message", passwordError.get());
        }

        boolean exists = customerRepository.findAll().stream()
                .anyMatch(customer -> customer.getEmail().equalsIgnoreCase(trimmedEmail));

        if (exists) {
            return Map.of("success", false, "message", "Email đã tồn tại");
        }

        String newId = customerRepository.generateNewCustomerId();
        Customer newCustomer = new Customer(newId, trimmedName, trimmedEmail,
                trimmedPhone, trimmedAddress, avatarUrl != null ? avatarUrl : "");
        newCustomer.setPassword(PasswordSecurity.hash(password));
        customerRepository.save(newCustomer);

        return Map.of("success", true, "message", "Đăng ký thành công", "customer", newCustomer);
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
        if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())
                || !PasswordSecurity.matches(password, customer.getPassword())) {
            return Optional.empty();
        }

        if (PasswordSecurity.needsRehash(customer.getPassword())) {
            customer.setPassword(PasswordSecurity.hash(password));
            customerRepository.save(customer);
        }
        return Optional.of(customer);
    }

    public Map<String, Object> updateProfile(String customerId, Map<String, String> payload) {
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            return Map.of("success", false, "message", "Không tìm thấy tài khoản.");
        }
        String name = payload.get("name");
        String phone = payload.get("phone");
        String address = payload.get("address");
        if (name != null && !name.trim().isEmpty()) {
            customer.setName(name.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            customer.setPhone(phone.trim());
        }
        if (address != null && !address.trim().isEmpty()) {
            customer.setAddress(address.trim());
        }
        customerRepository.save(customer);
        return Map.of("success", true, "message", "Cập nhật thông tin thành công!", "customer", customer);
    }

    public Map<String, Object> changePassword(String customerId, String oldPassword, String newPassword) {
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            return Map.of("success", false, "message", "Không tìm thấy tài khoản.");
        }
        if (!PasswordSecurity.matches(oldPassword, customer.getPassword())) {
            return Map.of("success", false, "message", "Mật khẩu cũ không đúng.");
        }
        Optional<String> passwordError = PasswordPolicy.validationError(newPassword);
        if (passwordError.isPresent()) {
            return Map.of("success", false, "message", passwordError.get());
        }
        customer.setPassword(PasswordSecurity.hash(newPassword));
        customerRepository.save(customer);
        return Map.of("success", true, "message", "Đổi mật khẩu thành công!");
    }

    public Map<String, Object> topUpWallet(String customerId, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            return Map.of("success", false, "message", "Số tiền nạp phải lớn hơn 0.");
        }
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            return Map.of("success", false, "message", "Không tìm thấy tài khoản.");
        }
        double updatedBalance = customer.getWalletBalance() + amount;
        if (!Double.isFinite(updatedBalance)) {
            return Map.of("success", false, "message", "Số tiền nạp vượt quá giới hạn cho phép.");
        }
        customer.setWalletBalance(updatedBalance);
        customerRepository.save(customer);
        return Map.of("success", true, "message",
                String.format("Nạp tiền thành công! Số dư hiện tại: %,.0f VND", customer.getWalletBalance()),
                "customer", customer);
    }
}