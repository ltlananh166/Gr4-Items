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

    public boolean register(String name, String email, String phone, String address, String password) {
        try {
            customerService.register(name, email, phone, address, password);
            System.out.println(">> Registration successful!");
            return true;
        } catch (AuthenticationException e) {
            System.err.println(">> Registration failed: " + e.getMessage());
            return false;
        }
    }

    public boolean login(String email, String password) {
        try {
            currentUser = customerService.login(email, password);
            System.out.println(">> Login successful! Welcome, " + currentUser.getName());
            System.out.println(">> Your membership tier: " + currentUser.getTier());
            return true;
        } catch (AuthenticationException e) {
            System.err.println(">> Login failed: " + e.getMessage());
            return false;
        }
    }

    public void logout() {
        if (currentUser != null) {
            System.out.println(">> Logged out of account: " + currentUser.getEmail());
            currentUser = null;
        }
    }

    public boolean updateProfile(String name, String phone, String address, String avatarUrl) {
        if (currentUser == null) {
            System.err.println(">> Profile update failed: Please log in first.");
            return false;
        }
        try {
            currentUser = customerService.updateProfile(
                    currentUser.getCustomerId(), name, phone, address, avatarUrl);
            System.out.println(">> Profile updated successfully.");
            return true;
        } catch (AuthenticationException e) {
            System.err.println(">> Profile update failed: " + e.getMessage());
            return false;
        }
    }

    public boolean changePassword(String currentPassword, String newPassword) {
        if (currentUser == null) {
            System.err.println(">> Password update failed: Please log in first.");
            return false;
        }
        try {
            customerService.changePassword(currentUser.getCustomerId(), currentPassword, newPassword);
            System.out.println(">> Password updated successfully.");
            return true;
        } catch (AuthenticationException e) {
            System.err.println(">> Password update failed: " + e.getMessage());
            return false;
        }
    }

    public boolean topUpWallet(double amount) {
        if (currentUser == null) {
            System.err.println(">> Wallet top-up failed: Please log in first.");
            return false;
        }
        try {
            double balance = customerService.topUpWallet(currentUser.getCustomerId(), amount);
            currentUser.setWalletBalance(balance);
            System.out.println(">> Wallet balance: " + balance);
            return true;
        } catch (FlashSaleRuleException e) {
            System.err.println(">> Wallet top-up failed: " + e.getMessage());
            return false;
        }
    }

    public double calculateTierDiscount(double amount) {
        try {
            return customerService.calculateTierDiscount(currentUser, amount);
        } catch (FlashSaleRuleException e) {
            System.err.println(">> Membership discount failed: " + e.getMessage());
            throw e;
        }
    }

    public double applyVoucher(String code, double amount) {
        try {
            double finalAmount = customerService.applyVoucher(currentUser, code, amount);
            System.out.println(">> Voucher applied. Discounted amount: $" + finalAmount);
            return finalAmount;
        } catch (FlashSaleRuleException e) {
            System.err.println(">> Voucher application failed: " + e.getMessage());
            throw e;
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