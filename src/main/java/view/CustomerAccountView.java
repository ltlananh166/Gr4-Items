package view;

import controller.CustomerController;
import model.Customer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Giao diện nhập/xuất dòng lệnh cho các thao tác xác thực và hồ sơ khách hàng. */
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
            Map<String, String> payload = new HashMap<>();
            payload.put("name", input.readStringRequired("Full name"));
            payload.put("email", input.readStringPattern("Email",
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", "Invalid email format"));
            payload.put("phone", input.readStringPattern("Phone number",
                    "^\\d{9,11}$", "Phone number must contain 9–11 digits"));
            payload.put("address", input.readStringRequired("Address"));
            payload.put("password", input.readStringRequired("Password (at least 6 characters)"));

            Map<String, Object> result = controller.register(payload);
            System.out.println(result.get("message"));
        } catch (exception.OperationCancelledException exception) {
            System.out.println("Registration cancelled.");
        }
    }

    public Optional<Customer> login() {
        System.out.println("\n--- CUSTOMER LOGIN ---");
        try {
            String email = input.readStringRequired("Email");
            String password = input.readStringRequired("Password");
            Optional<Customer> customer = controller.login(
                    Map.of("email", email, "password", password));
            System.out.println(customer.isPresent()
                    ? "Login successful."
                    : "Login failed. Check your email, password, or account status.");
            return customer;
        } catch (exception.OperationCancelledException exception) {
            System.out.println("Login cancelled.");
            return Optional.empty();
        }
    }

    public Customer displayAccountMenu(Customer customer) {
        Customer current = customer;
        boolean back = false;
        while (!back) {
            printAccountMenu();
            int choice = input.readInt("Select an option", 0);
            switch (choice) {
                case 1 -> displayProfile(current);
                case 2 -> current = updateProfile(current);
                case 3 -> changePassword(current);
                case 4 -> current = topUpWallet(current);
                case 0 -> back = true;
                default -> System.out.println("Invalid choice.");
            }
        }
        return current;
    }

    public void printAccountMenu() {
        System.out.println("\n--- MY ACCOUNT ---");
        System.out.println("1. View profile");
        System.out.println("2. Update profile");
        System.out.println("3. Change password");
        System.out.println("4. Top up wallet");
        System.out.println("0. Back");
    }

    public void displayProfile(Customer customer) {
        System.out.println("Customer ID: " + customer.getCustomerId());
        System.out.println("Name: " + customer.getName());
        System.out.println("Email: " + customer.getEmail());
        System.out.println("Phone number: " + customer.getPhone());
        System.out.println("Address: " + customer.getAddress());
        System.out.println("Tier: " + customer.getTier());
        System.out.println("Status: " + customer.getStatus());
        System.out.printf("Wallet balance: %,.0f VND%n", customer.getWalletBalance());
    }

    public Customer updateProfile(Customer customer) {
        try {
            String name = input.readLine("New name (leave blank to keep current): ");
            String phone = input.readLine("New phone number (leave blank to keep current): ");
            String address = input.readLine("New address (leave blank to keep current): ");
            Map<String, Object> result = controller.updateProfile(customer.getCustomerId(),
                    Map.of("name", name, "phone", phone, "address", address));
            System.out.println(result.get("message"));
            return Boolean.TRUE.equals(result.get("success"))
                    ? (Customer) result.get("customer") : customer;
        } catch (exception.OperationCancelledException exception) {
            System.out.println("Profile update cancelled.");
            return customer;
        }
    }

    public void changePassword(Customer customer) {
        try {
            String oldPassword = input.readStringRequired("Current password");
            String newPassword = input.readStringRequired("New password");
            String confirmation = input.readStringRequired("Confirm new password");
            if (!newPassword.equals(confirmation)) {
                System.out.println("Passwords do not match.");
                return;
            }
            Map<String, Object> result = controller.changePassword(
                    customer.getCustomerId(), oldPassword, newPassword);
            System.out.println(result.get("message"));
        } catch (exception.OperationCancelledException exception) {
            System.out.println("Password change cancelled.");
        }
    }

    public Customer topUpWallet(Customer customer) {
        try {
            double amount = input.readDouble("Top-up amount", 100_000);
            Map<String, Object> result = controller.topUpWallet(customer.getCustomerId(), amount);
            System.out.println(result.get("message"));
            return Boolean.TRUE.equals(result.get("success"))
                    ? (Customer) result.get("customer") : customer;
        } catch (exception.OperationCancelledException exception) {
            System.out.println("Wallet top-up cancelled.");
            return customer;
        }
    }
}

// Thành viên 3
