package view;

import controller.CustomerController;
import java.util.Scanner;

public class CustomerView {
    private final CustomerController customerController;
    private final Scanner scanner;

    public CustomerView(CustomerController customerController) {
        this.customerController = customerController;
        this.scanner = new Scanner(System.in);
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n========== CUSTOMER ACCOUNT MANAGEMENT ==========");
            if (customerController.getCurrentUser() == null) {
                System.out.println("1. Register\n2. Login\n0. Back to Main Menu");
            } else {
                System.out.println("Account: " + customerController.getCurrentUser().getUsername()
                        + " | Tier: " + customerController.getCurrentUser().getTier());
                System.out.println("1. Apply voucher\n2. Check Flash Sale quantity\n3. Logout\n0. Back to Main Menu");
            }
            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();
            if (customerController.getCurrentUser() == null) {
                switch (choice) {
                    case "1": handleRegister(); break;
                    case "2": handleLogin(); break;
                    case "0": return;
                    default: System.out.println("Invalid choice. Please try again.");
                }
            } else {
                switch (choice) {
                    case "1": handleApplyVoucher(); break;
                    case "2": handleFlashSaleQuantity(); break;
                    case "3": customerController.logout(); break;
                    case "0": return;
                    default: System.out.println("Invalid choice. Please try again.");
                }
            }
        }
    }

    private void handleRegister() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        customerController.register(username, scanner.nextLine());
    }

    private void handleLogin() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        customerController.login(username, scanner.nextLine());
    }

    private void handleApplyVoucher() {
        System.out.print("Enter voucher code: ");
        String code = scanner.nextLine();
        System.out.print("Enter order amount: $");
        try {
            customerController.applyVoucher(code, Double.parseDouble(scanner.nextLine().trim()));
        } catch (NumberFormatException e) {
            System.err.println("Order amount must be a valid number.");
        }
    }

    private void handleFlashSaleQuantity() {
        System.out.print("Enter quantity: ");
        try {
            customerController.checkFlashSaleLimit(Integer.parseInt(scanner.nextLine().trim()));
        } catch (NumberFormatException e) {
            System.err.println("Quantity must be a whole number.");
        }
    }
}