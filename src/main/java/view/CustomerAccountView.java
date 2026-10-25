package view;

import controller.CustomerController;
import model.Customer;

import java.util.Optional;

 
public class CustomerAccountView {

    private final CustomerController controller;
    private final ConsoleInput input;

    public CustomerAccountView(CustomerController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }


    public void register() {
        System.out.println("\n--- CUSTOMER REGISTRATION ---");
        try {
            String name = input.readStringRequired("Full name: ");
            String email = input.readStringPattern("Email: ",
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", "Invalid email format!");
            String phone = input.readStringPattern("Phone number (10 digits): ",
                    "^0\\d{9}$", "Phone number must start with 0 and contain exactly 10 digits!");
            String address = input.readStringRequired("Address: ");
            String password = input.readStringPattern("Password (min 8 characters): ",
                    "^.{8,}$", "Password must contain at least 8 characters! Please enter again.");

            controller.register(name, email, phone, address, password);
        } catch (Exception exception) {
            System.out.println("Registration error: " + exception.getMessage());
        }
    }


    public Optional<Customer> login() {
        System.out.println("\n--- CUSTOMER LOGIN ---");
        try {
            String email = input.readStringRequired("Email: ");
            String password = input.readStringRequired("Password: ");

            boolean success = controller.login(email, password);
            if (success) {
                return Optional.ofNullable(controller.getCurrentUser());
            }
            return Optional.empty();
        } catch (Exception exception) {
            System.out.println("Login error: " + exception.getMessage());
            return Optional.empty();
        }
    }

 
    public Customer displayAccountMenu(Customer customer) {
        Customer current = customer;
        boolean back = false;
        while (!back) {
            printAccountMenu();
            int choice = input.readInt("Choose an option", 0);
            switch (choice) {
                case 1 -> displayProfile(current);
                case 2 -> current = updateProfile(current);
                case 3 -> changePassword(current);
                case 4 -> current = topUpWallet(current);
                case 0 -> back = true;
                default -> System.out.println("Invalid choice. Please try again!");
            }
        }
        return current;
    }


    public void printAccountMenu() {
        System.out.println("\n===== MY ACCOUNT PROFILE =====");
        System.out.println("1. View Profile");
        System.out.println("2. Update Profile");
        System.out.println("3. Change Password");
        System.out.println("4. Top-up Wallet Balance");
        System.out.println("0. Back to Main Menu");
        System.out.println("==============================");
    }

 
    public void displayProfile(Customer customer) {
        System.out.println("\n--- PERSONAL PROFILE ---");
        System.out.println("Customer ID:    " + customer.getCustomerId());
        System.out.println("Full Name:      " + customer.getName());
        System.out.println("Email:          " + customer.getEmail());
        System.out.println("Phone:          " + customer.getPhone());
        System.out.println("Address:        " + customer.getAddress());
        System.out.println("VIP Tier:       " + customer.getTier());
        System.out.println("Account Status: " + customer.getStatus());
        System.out.printf("Wallet Balance: %,.0f VND%n", customer.getWalletBalance());
        System.out.println("------------------------");
    }

    public Customer updateProfile(Customer customer) {
        System.out.println("\n--- UPDATE PROFILE ---");
        String name = input.readStringRequired("New full name: ");
        String phone = input.readStringPattern("New phone number (10 digits): ",
                "^0\\d{9}$", "Phone number must start with 0 and contain exactly 10 digits!");
        String address = input.readStringRequired("New address: ");
        String avatar = input.readLine("New avatar URL (optional): ");

        boolean success = controller.updateProfile(name, phone, address, avatar);
        if (success) {
            return controller.getCurrentUser();
        }
        return customer;
    }

    public void changePassword(Customer customer) {
        System.out.println("\n--- CHANGE PASSWORD ---");
        String currentPass = input.readStringRequired("Current password: ");
        String newPass = input.readStringPattern("New password (min 8 characters): ",
                "^.{8,}$", "Password must contain at least 8 characters! Please enter again.");
        controller.changePassword(currentPass, newPass);
    }


    public Customer topUpWallet(Customer customer) {
        System.out.println("\n--- TOP-UP WALLET ---");
        double amount = input.readDouble("Enter top-up amount", 500000);
        if (amount <= 0) {
            System.out.println("Top-up amount must be greater than 0!");
            return customer;
        }

        boolean success = controller.topUpWallet(amount);
        if (success) {
            return controller.getCurrentUser();
        }
        return customer;
    }
}