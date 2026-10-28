package view;

import model.Customer;
import repository.CustomerRepository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AdminCustomerView {
    private final CustomerRepository customers;
    private final ConsoleInput input;

    public AdminCustomerView(CustomerRepository customers, ConsoleInput input) {
        this.customers = Objects.requireNonNull(customers, "customers");
        this.input = Objects.requireNonNull(input, "input");
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n===== CUSTOMER MANAGEMENT =====");
            System.out.println("1. List customers");
            System.out.println("2. Search customer");
            System.out.println("3. Ban / unban customer");
            System.out.println("0. Back");
            int choice = input.readIntMinMax("Choose an option", 0, 3,
                    "Choose a number from 0 to 3.");
            switch (choice) {
                case 1 -> displayCustomers(customers.getAll());
                case 2 -> searchCustomer();
                case 3 -> toggleStatus();
                case 0 -> back = true;
                default -> throw new IllegalStateException("Validated customer menu choice was out of range.");
            }
        }
    }

    private void searchCustomer() {
        String query = input.readStringRequired("Customer ID, name, or email: ")
                .toLowerCase(Locale.ROOT);
        displayCustomers(customers.getAll().stream()
                .filter(customer -> contains(customer.getCustomerId(), query)
                        || contains(customer.getName(), query)
                        || contains(customer.getEmail(), query))
                .limit(50)
                .toList());
    }

    private void toggleStatus() {
        String customerId = input.readStringRequired("Customer ID: ");
        Customer customer = customers.findById(customerId).orElse(null);
        if (customer == null) {
            System.out.println("Customer not found: " + customerId);
            return;
        }
        if (customer.isActive()) customer.ban();
        else customer.unban();
        customers.save(customer);
        System.out.println("Customer " + customerId + " is now " + customer.getStatus() + ".");
    }

    private void displayCustomers(List<Customer> items) {
        System.out.printf("Showing %d customer(s).%n", items.size());
        System.out.printf("%-10s %-24s %-32s %-12s %-10s%n",
                "ID", "Name", "Email", "Tier", "Status");
        for (Customer customer : items.stream().limit(50).toList()) {
            System.out.printf("%-10s %-24s %-32s %-12s %-10s%n",
                    customer.getCustomerId(), trim(customer.getName(), 24),
                    trim(customer.getEmail(), 32), customer.getTier(), customer.getStatus());
        }
        if (items.size() > 50) System.out.println("Showing first 50 results.");
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value == null ? "" : value;
        return value.substring(0, maxLength - 3) + "...";
    }
}
