package service;

import exception.AuthenticationException;
import exception.FlashSaleRuleException;
import java.util.Optional;
import model.Customer;
import model.Voucher;
import model.enums.CustTier;
import model.enums.CustomerStatus;
import repository.CustomerRepository;
import repository.VoucherRepository;
import security.PasswordSecurity;

public class CustomerService {
    private static final int MAX_FLASH_SALE_QUANTITY = 2;

    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;

    public CustomerService(CustomerRepository customerRepository, VoucherRepository voucherRepository) {
        this.customerRepository = customerRepository;
        this.voucherRepository = voucherRepository;
    }

    public Customer register(String username, String password) {
        return registerAccount(username, username, "", "", "", password, false);
    }

    public Customer register(String name, String email, String phone, String address, String password) {
        return registerAccount(name, email, phone, address, "", password, true);
    }

    private Customer registerAccount(String name, String email, String phone,
            String address, String avatarUrl, String password, boolean validateEmail) {
        requireText(name, "Name");
        requireText(email, "Email");
        if (validateEmail) {
            requireText(phone, "Phone");
        }
        if (validateEmail && !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new AuthenticationException("Email address is invalid.");
        }
        validatePassword(password);
        String normalizedEmail = email.trim();
        if (customerRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new AuthenticationException("Email already exists.");
        }
        Customer customer = new Customer(
                customerRepository.generateNewCustomerId(),
                name.trim(),
                normalizedEmail,
                phone.trim(),
                address == null ? "" : address.trim(),
                avatarUrl,
                CustTier.STANDARD,
                "ACTIVE",
                PasswordSecurity.hash(password),
                0.0);
        customerRepository.save(customer);
        return customer;
    }

    public Customer login(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Email and password are required.");
        }
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Account does not exist."));
        if (customer.getStatus() == CustomerStatus.BANNED) {
            throw new AuthenticationException("This account has been banned.");
        }
        if (!PasswordSecurity.supports(customer.getPassword())
                && PasswordSecurity.isEncodedHash(customer.getPassword())) {
            throw new AuthenticationException("Password hash format is unsupported; reset the password.");
        }
        if (!PasswordSecurity.matches(password, customer.getPassword())) {
            throw new AuthenticationException("Incorrect password.");
        }
        if (!PasswordSecurity.isEncodedHash(customer.getPassword())) {
            customerRepository.save(customer);
        }
        return customer;
    }

    public Customer updateProfile(String customerId, String name, String phone,
            String address, String avatarUrl) {
        Customer customer = requireCustomer(customerId);
        requireText(name, "Name");
        requireText(phone, "Phone");
        customer.setName(name.trim());
        customer.setPhone(phone.trim());
        customer.setAddress(address == null ? "" : address.trim());
        customer.setAvatarUrl(avatarUrl == null ? "" : avatarUrl.trim());
        customerRepository.save(customer);
        return customer;
    }

    public void changePassword(String customerId, String currentPassword, String newPassword) {
        Customer customer = requireCustomer(customerId);
        if (!PasswordSecurity.supports(customer.getPassword())
                && PasswordSecurity.isEncodedHash(customer.getPassword())) {
            throw new AuthenticationException("Password hash format is unsupported; reset the password.");
        }
        if (!PasswordSecurity.matches(currentPassword, customer.getPassword())) {
            throw new AuthenticationException("Current password is incorrect.");
        }
        validatePassword(newPassword);
        customer.setPassword(PasswordSecurity.hash(newPassword));
        customerRepository.save(customer);
    }

    public double topUpWallet(String customerId, double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new FlashSaleRuleException("Top-up amount must be a finite positive number.");
        }
        Customer customer = requireCustomer(customerId);
        double newBalance = customer.getWalletBalance() + amount;
        if (Double.isInfinite(newBalance)) {
            throw new FlashSaleRuleException("Wallet balance exceeds the supported limit.");
        }
        customer.setWalletBalance(newBalance);
        customerRepository.save(customer);
        return newBalance;
    }

    public double calculateTierDiscount(Customer customer, double originalAmount) {
        if (customer == null) {
            throw new FlashSaleRuleException("Please log in before using membership benefits.");
        }
        validateAmount(originalAmount);
        return originalAmount * customer.getTier().getDiscountRate();
    }

    public double applyVoucher(Customer customer, String voucherCode, double originalAmount) {
        if (customer == null) {
            throw new FlashSaleRuleException("Please log in before applying a voucher.");
        }
        validateAmount(originalAmount);
        Voucher voucher = voucherRepository.findByCode(voucherCode)
                .orElseThrow(() -> new FlashSaleRuleException("Voucher code does not exist."));
        if (!voucher.isValid()) {
            throw new FlashSaleRuleException("This voucher is not currently valid.");
        }
        java.math.BigDecimal orderAmount = java.math.BigDecimal.valueOf(originalAmount);
        if (orderAmount.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new FlashSaleRuleException("Order does not meet the voucher minimum.");
        }
        double discount = voucher.calculateDiscount(orderAmount).doubleValue();
        return Math.max(0.0, originalAmount - discount);
    }

    public void validateFlashSalePurchase(int quantity) {
        if (quantity < 1 || quantity > MAX_FLASH_SALE_QUANTITY) {
            throw new FlashSaleRuleException(
                    "Flash Sale quantity must be between 1 and " + MAX_FLASH_SALE_QUANTITY + ".");
        }
    }

    private Customer requireCustomer(String customerId) {
        Optional<Customer> customer = customerRepository.findById(customerId);
        return customer.orElseThrow(() -> new AuthenticationException("Customer does not exist."));
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new AuthenticationException(fieldName + " cannot be empty.");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new AuthenticationException("Password must contain at least 8 characters.");
        }
    }

    private static void validateAmount(double amount) {
        if (amount < 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new FlashSaleRuleException("Order amount must be a valid non-negative number.");
        }
    }
}
