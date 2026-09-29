package service;

import exception.AuthenticationException;
import exception.FlashSaleRuleException;
import java.util.Objects;
import java.util.UUID;
import model.Customer;
import model.Voucher;
import model.enums.CustTier;
import repository.CustomerRepository;
import repository.VoucherRepository;

public class CustomerService {
    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;

    public CustomerService(CustomerRepository customerRepository, VoucherRepository voucherRepository) {
        this.customerRepository = customerRepository;
        this.voucherRepository = voucherRepository;
    }

    public Customer register(String username, String password) {
        if (username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            throw new AuthenticationException("Username and password cannot be empty.");
        }
        if (customerRepository.findByUsername(username).isPresent()) {
            throw new AuthenticationException("Username already exists.");
        }
        Customer customer = new Customer(UUID.randomUUID().toString(), username.trim(), password,
                "", "", CustTier.STANDARD, 0.0);
        customerRepository.save(customer);
        return customer;
    }

    public Customer login(String username, String password) {
        Customer customer = customerRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Account does not exist."));
        if (!Objects.equals(customer.getPassword(), password)) {
            throw new AuthenticationException("Incorrect password.");
        }
        return customer;
    }

    public double applyVoucher(Customer customer, String voucherCode, double originalAmount) {
        if (customer == null) {
            throw new FlashSaleRuleException("Please log in before applying a voucher.");
        }
        if (originalAmount < 0 || Double.isNaN(originalAmount) || Double.isInfinite(originalAmount)) {
            throw new FlashSaleRuleException("Order amount must be a valid non-negative number.");
        }
        Voucher voucher = voucherRepository.findByCode(voucherCode)
                .orElseThrow(() -> new FlashSaleRuleException("Voucher code does not exist."));
        if (voucher.isUsed()) {
            throw new FlashSaleRuleException("This voucher has already been used.");
        }
        if (originalAmount < voucher.getMinOrderValue()) {
            throw new FlashSaleRuleException("Order does not meet the voucher minimum.");
        }
        if (customer.getTier().ordinal() < voucher.getRequiredTier().ordinal()) {
            throw new FlashSaleRuleException("Your membership tier is not eligible for this voucher.");
        }
        if (voucher.getDiscountAmount() < 0) {
            throw new FlashSaleRuleException("Voucher discount cannot be negative.");
        }
        voucher.setUsed(true);
        voucherRepository.update(voucher);
        return Math.max(0.0, originalAmount - voucher.getDiscountAmount());
    }

    public void validateFlashSalePurchase(int quantity) {
        if (quantity < 1 || quantity > 2) {
            throw new FlashSaleRuleException("Flash Sale quantity must be between 1 and 2.");
        }
    }
}