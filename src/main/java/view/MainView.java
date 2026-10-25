package view;

import controller.CustomerController;
import model.Customer;
import model.FlashSaleItem;
import repository.FlashSaleItemRepository;

import java.util.List;

public class MainView {
    private final ConsoleInput input;
    private final FlashSaleView flashSaleView;
    private final FlashSaleItemRepository itemRepo;
    private final AdminView adminView;
    private final CustomerAccountView accountView;
    private final CustomerController customerController;

    private Customer loggedInCustomer;

    public MainView(ConsoleInput input,
                    FlashSaleView flashSaleView,
                    FlashSaleItemRepository itemRepo,
                    AdminView adminView,
                    CustomerAccountView accountView,
                    CustomerController customerController) {
        this.input = input;
        this.flashSaleView = flashSaleView;
        this.itemRepo = itemRepo;
        this.adminView = adminView;
        this.accountView = accountView;
        this.customerController = customerController;
    }

    public void display() {
        boolean running = true;
        while (running) {
            refreshSession();
            printMenu();
            int choice = input.readInt("Please choose an option", 0);
            running = loggedInCustomer == null
                    ? handleGuestChoice(choice)
                    : handleCustomerChoice(choice);
        }
        System.out.println("\nThank you for using Flash Sale System! Goodbye!");
    }

    private void printMenu() {
        System.out.println("\n================ FLASH SALE SYSTEM =================");
        if (loggedInCustomer == null) {
            System.out.println("Status: Guest (Not logged in)");
            System.out.println("1. Browse Flash Sale products");
            System.out.println("2. Register customer account");
            System.out.println("3. Login customer");
            System.out.println("4. Admin Login");
        } else {
            System.out.printf("Logged in: %s (%s) | Tier: %s | Wallet: %,.0f VND%n",
                    loggedInCustomer.getName(), loggedInCustomer.getCustomerId(),
                    loggedInCustomer.getTier(), loggedInCustomer.getWalletBalance());
            System.out.println("1. Browse Flash Sale products");
            System.out.println("2. My Account & Wallet (Profile / Password / Top-up)");
            System.out.println("3. Logout");
        }
        System.out.println("0. Exit");
        System.out.println("=====================================================");
    }

    private boolean handleGuestChoice(int choice) {
        switch (choice) {
            case 1 -> browseProducts();
            case 2 -> accountView.register();
            case 3 -> loggedInCustomer = accountView.login().orElse(null);
            case 4 -> openAdmin();
            case 0 -> {
                return false;
            }
            default -> System.out.println("Invalid choice. Please try again!");
        }
        return true;
    }

    private boolean handleCustomerChoice(int choice) {
        switch (choice) {
            case 1 -> browseProducts();
            case 2 -> loggedInCustomer = accountView.displayAccountMenu(loggedInCustomer);
            case 3 -> logout();
            case 0 -> {
                return false;
            }
            default -> System.out.println("Invalid choice. Please try again!");
        }
        return true;
    }

    private void browseProducts() {
        System.out.println("\n--- CURRENT FLASH SALE PRODUCTS ---");
        FlashSaleItem item = itemRepo.finndById("ITEM001");
        if (item != null) {
            flashSaleView.displayItems(List.of(item));
        } else {
            System.out.println("No products available at the moment.");
        }
    }

    private void openAdmin() {
        System.out.println("\n--- ADMIN AUTHENTICATION ---");
        String username = input.readStringRequired("Admin username: ");
        String password = input.readStringRequired("Admin password: ");

        if ("admin".equals(username) && "admin123".equals(password)) {
            System.out.println(">> Login successful! Welcome Administrator.");
            adminView.display();
        } else {
            System.out.println(">> Invalid admin credentials! Access denied.");
        }
    }

    private void refreshSession() {
        if (loggedInCustomer != null && customerController.getCurrentUser() != null) {
            loggedInCustomer = customerController.getCurrentUser();
        }
    }

    private void logout() {
        customerController.logout();
        loggedInCustomer = null;
        System.out.println(">> Logged out successfully.");
    }
}