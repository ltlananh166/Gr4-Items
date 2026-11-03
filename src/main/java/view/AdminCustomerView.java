package view;

import controller.AdminCustomerController;
import exception.OperationCancelledException;
import model.Customer;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/** Giao diện dòng lệnh chỉ dùng để quản lý khách hàng dành cho quản trị viên. */
public class AdminCustomerView {
    private final AdminCustomerController controller;
    private final ConsoleInput input;

    public AdminCustomerView(AdminCustomerController controller) {
        this(controller, new ConsoleInput(new Scanner(System.in)));
    }

    public AdminCustomerView(AdminCustomerController controller, ConsoleInput input) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.input = Objects.requireNonNull(input, "input");
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Select an option", 0)) {
                    case 1 -> showCustomers(controller.list());
                    case 2 -> search();
                    case 3 -> create();
                    case 4 -> update();
                    case 5 -> delete();
                    case 6 -> changeStatus("BANNED");
                    case 7 -> changeStatus("ACTIVE");
                    case 0 -> back = true;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (OperationCancelledException exception) {
                System.out.println("Operation cancelled.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n===== CUSTOMER ACCOUNT MANAGEMENT =====");
        System.out.println("1. List customers");
        System.out.println("2. Search customers");
        System.out.println("3. Add customer");
        System.out.println("4. Update customer");
        System.out.println("5. Delete customer");
        System.out.println("6. Ban account");
        System.out.println("7. Unban account");
        System.out.println("0. Back");
    }

    private void search() {
        String keyword = input.readStringRequired("Enter customer ID, name, email, or phone number");
        showCustomers(controller.search(keyword));
    }

    private void create() {
        String name = input.readStringRequired("Name");
        String email = input.readStringRequired("Email");
        String phone = input.readStringRequired("Phone number");
        String address = input.readStringRequired("Address");
        String tier = input.readLine("Tier STANDARD/SILVER/GOLD/DIAMOND [STANDARD]: ");
        String password = input.readStringRequired("Password");
        printAction(controller.create(name, email, phone, address, tier, password));
    }

    private void update() {
        String customerId = input.readStringRequired("Customer ID to update");
        System.out.println("Leave fields blank to keep current values.");
        String name = input.readLine("New name: ");
        String email = input.readLine("New email: ");
        String phone = input.readLine("New phone number: ");
        String address = input.readLine("New address: ");
        String tier = input.readLine("New tier: ");
        String password = input.readLine("New password: ");
        printAction(controller.update(customerId, name, email, phone, address,
                tier, password));
    }

    private void delete() {
        String customerId = input.readStringRequired("Customer ID to delete");
        String confirmation = input.readLine(
                "Delete this account and cancel unfinished orders? Enter Y to confirm: ");
        if (!"Y".equalsIgnoreCase(confirmation)) {
            System.out.println("Deletion cancelled.");
            return;
        }
        printAction(controller.delete(customerId));
    }

    private void changeStatus(String status) {
        String customerId = input.readStringRequired("Customer ID");
        printAction(controller.setStatus(customerId, status));
    }

    private void showCustomers(AdminCustomerController.Result<List<Customer>> result) {
        if (!result.success()) {
            System.out.println("Error: " + result.message());
            return;
        }
        List<Customer> customers = result.data();
        if (customers == null || customers.isEmpty()) {
            System.out.println("No matching customers found.");
            return;
        }
        System.out.printf("%-10s | %-20s | %-28s | %-12s | %-9s | %-8s%n",
                "ID", "Name", "Email", "Phone", "Tier", "Status");
        System.out.println("------------------------------------------------------------------------------------------------");
        for (Customer customer : customers) {
            System.out.printf("%-10s | %-20s | %-28s | %-12s | %-9s | %-8s%n",
                    text(customer.getCustomerId(), 10), text(customer.getName(), 20),
                    text(customer.getEmail(), 28), text(customer.getPhone(), 12),
                    customer.getTier(), text(customer.getStatus().name(), 8));
        }
    }

    private void printAction(AdminCustomerController.Result<?> result) {
        System.out.println((result.success() ? "Success: " : "Error: ") + result.message());
    }

    private static String text(String value, int width) {
        String safe = value == null ? "" : value;
        return safe.length() <= width ? safe : safe.substring(0, width - 3) + "...";
    }
}

// Thành viên 3
