package view;

import controller.CustomerController;
import model.Customer;
import model.FlashSaleEvent;
import model.FlashSaleItem;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MainView {
    private final ConsoleInput input;
    private final FlashSaleView flashSaleView;
    private final FlashSaleItemRepository itemRepo;
    private final FlashSaleEventRepository eventRepo;
    private final AdminView adminView;
    private final CustomerAccountView accountView;
    private final CustomerController customerController;
    private final SimulatorView simulatorView;

    private Customer loggedInCustomer;

    public MainView(ConsoleInput input,
                    FlashSaleView flashSaleView,
                    FlashSaleItemRepository itemRepo,
                    FlashSaleEventRepository eventRepo,
                    AdminView adminView,
                    CustomerAccountView accountView,
                    CustomerController customerController,
                    SimulatorView simulatorView) {
        this.input = input;
        this.flashSaleView = flashSaleView;
        this.itemRepo = itemRepo;
        this.eventRepo = eventRepo;
        this.adminView = adminView;
        this.accountView = accountView;
        this.customerController = customerController;
        this.simulatorView = simulatorView;
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
            System.out.println("5. Researcher Simulator");
        } else {
            System.out.printf("Logged in: %s (%s) | Tier: %s | Wallet: %,.0f VND%n",
                    loggedInCustomer.getName(), loggedInCustomer.getCustomerId(),
                    loggedInCustomer.getTier(), loggedInCustomer.getWalletBalance());
            System.out.println("1. Browse Flash Sale products");
            System.out.println("2. My Account & Wallet (Profile / Password / Top-up)");
            System.out.println("3. Logout");
            System.out.println("4. Researcher Simulator");
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
            case 5 -> simulatorView.start();
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
            case 4 -> simulatorView.start();
            case 0 -> {
                return false;
            }
            default -> System.out.println("Invalid choice. Please try again!");
        }
        return true;
    }

    private void browseProducts() {
        System.out.println("\n--- CURRENT FLASH SALE PRODUCTS ---");
        Set<String> activeEventIds = eventRepo.findActiveEvents().stream()
                .map(FlashSaleEvent::getEventId)
                .collect(Collectors.toSet());
        List<FlashSaleItem> items = itemRepo.findAll().stream()
                .filter(item -> activeEventIds.contains(item.getEventId()))
                .filter(FlashSaleItem::isAvailable)
                .toList();
        if (items.size() > 20) {
            System.out.printf("Showing 20 of %d active items.%n", items.size());
            items = items.subList(0, 20);
        }
        flashSaleView.displayItems(items);
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
