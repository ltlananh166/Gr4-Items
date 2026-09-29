package controller;

import exception.AuthenticationException;
import exception.FlashSaleRuleException;
import model.Customer;
import service.CustomerService;

public class CustomerController {
    private final CustomerService customerService;
    private Customer currentUser;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    public boolean register(String username, String password) {
        try {
            customerService.register(username, password);
            System.out.println(">> Registration successful!");
            return true;
        } catch (AuthenticationException e) {
            System.err.println(">> Registration failed: " + e.getMessage());
            return false;
        }
    }

    public boolean login(String username, String password) {
        try {
            currentUser = customerService.login(username, password);
            System.out.println(">> Login successful! Welcome, " + currentUser.getUsername());
            System.out.println(">> Your membership tier: " + currentUser.getTier());
            return true;
        } catch (AuthenticationException e) {
            System.err.println(">> Login failed: " + e.getMessage());
            return false;
        }
    }

    public void logout() {
        if (currentUser != null) {
            System.out.println(">> Logged out of account: " + currentUser.getUsername());
            currentUser = null;
        }
    }

    public double applyVoucher(String code, double amount) {
        try {
            double finalAmount = customerService.applyVoucher(currentUser, code, amount);
            System.out.println(">> Voucher applied. Discounted amount: $" + finalAmount);
            return finalAmount;
        } catch (FlashSaleRuleException e) {
            System.err.println(">> Voucher application failed: " + e.getMessage());
            return amount;
        }
    }

    public boolean checkFlashSaleLimit(int quantity) {
        try {
            customerService.validateFlashSalePurchase(quantity);
            return true;
        } catch (FlashSaleRuleException e) {
            System.err.println(">> Rule check failed: " + e.getMessage());
            return false;
        }
    }

    public Customer getCurrentUser() {
        return currentUser;
    }
}