package view;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import controller.CustomerController;
import controller.DataController;
import exception.EndOfInputException;
import exception.OperationCancelledException;
import model.Customer;
import security.AdminCredentials;
import security.SecurityEnvironment;

/** Bộ định tuyến giao diện dòng lệnh cấp cao nhất. Logic nghiệp vụ nằm trong các bộ điều khiển và dịch vụ. */
public class MainView {
    private static final int MAX_ADMIN_FAILURES = 3;
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final CustomerController customerController;
    private final DataController dataController;
    private final CustomerAccountView accountView;
    private final FlashSaleShoppingView shoppingView;
    private final CartView cartView;
    private final OrderTrackingView trackingView;
    private final CustomerVoucherView voucherView;
    private final AdminView adminView;
    private final ResearcherView researcherView;
    private final ReportView reportView;
    private final Optional<AdminCredentials> adminCredentials;
    private final ConsoleInput input;

    private Customer loggedInCustomer;
    private int failedAdminAttempts;

    public MainView(CustomerController customerController,
                    DataController dataController,
                    CustomerAccountView accountView,
                    FlashSaleShoppingView shoppingView,
                    OrderTrackingView trackingView,
                    CustomerVoucherView voucherView,
                    AdminView adminView,
                    ResearcherView researcherView,
                    ReportView reportView,
                    Optional<AdminCredentials> adminCredentials,
                    ConsoleInput input) {
        this(customerController, dataController, accountView, shoppingView, null,
                trackingView, voucherView, adminView, researcherView, reportView,
                adminCredentials, input);
    }

    public MainView(CustomerController customerController,
                    DataController dataController,
                    CustomerAccountView accountView,
                    FlashSaleShoppingView shoppingView,
                    CartView cartView,
                    OrderTrackingView trackingView,
                    CustomerVoucherView voucherView,
                    AdminView adminView,
                    ResearcherView researcherView,
                    ReportView reportView,
                    Optional<AdminCredentials> adminCredentials,
                    ConsoleInput input) {
        this.customerController = customerController;
        this.dataController = dataController;
        this.accountView = accountView;
        this.shoppingView = shoppingView;
        this.cartView = cartView;
        this.trackingView = trackingView;
        this.voucherView = voucherView;
        this.adminView = adminView;
        this.researcherView = researcherView;
        this.reportView = reportView;
        this.adminCredentials = adminCredentials;
        this.input = input;
    }

    public void display() {
        boolean running = true;
        while (running) {
            refreshSession();
            printMenu();
            try {
                int choice = input.readInt("Select an option", 0);
                running = loggedInCustomer == null
                        ? handleGuestChoice(choice)
                        : handleCustomerChoice(choice);
            } catch (EndOfInputException exception) {
                running = false;
            } catch (OperationCancelledException exception) {
                System.out.println("Operation cancelled. Returning to the main menu.");
            }
        }
        System.out.println("Application exited.");
    }

    public boolean handleGuestChoice(int choice) {
        switch (choice) {
            case 1 -> shoppingView.browse();
            case 2 -> accountView.register();
            case 3 -> loggedInCustomer = accountView.login().orElse(null);
            case 4 -> openAdmin();
            case 5 -> researcherView.display();
            case 0 -> {
                return false;
            }
            default -> System.out.println("Invalid choice.");
        }
        return true;
    }

    public boolean handleCustomerChoice(int choice) {
        switch (choice) {
            case 1 -> shoppingView.browse(loggedInCustomer);
            case 2 -> {
                shoppingView.book(loggedInCustomer);
                refreshSession();
            }
            case 3 -> trackingView.display(loggedInCustomer.getCustomerId());
            case 4 -> loggedInCustomer = accountView.displayAccountMenu(loggedInCustomer);
            case 5 -> voucherView.display();
            case 6 -> logout();
            case 7 -> {
                if (cartView == null) {
                    System.out.println("Cart is not available.");
                } else {
                    cartView.display(loggedInCustomer.getCustomerId());
                }
            }
            case 0 -> {
                return false;
            }
            default -> System.out.println("Invalid choice.");
        }
        return true;
    }

    public void printMenu() {
        System.out.println("\n===== LAB211 FLASH SALE CONSOLE =====");
        if (loggedInCustomer == null) {
            System.out.println("Status: Not logged in");
            System.out.println("1. Browse Flash Sale events and products");
            System.out.println("2. Register customer account");
            System.out.println("3. Customer login");
            System.out.println("4. Admin login");
            System.out.println("5. Concurrency simulation lab");
        } else {
            System.out.printf("Customer: %s (%s) | Tier: %s | Wallet: %,.0f VND%n",
                    loggedInCustomer.getName(), loggedInCustomer.getCustomerId(),
                    loggedInCustomer.getTier(), loggedInCustomer.getWalletBalance());
            System.out.println("1. Browse Flash Sale events and products");
            System.out.println("2. Place Flash Sale order");
            System.out.println("3. Track orders");
            System.out.println("4. Profile, password, and wallet");
            System.out.println("5. Available vouchers");
            System.out.println("6. Log out");
            System.out.println("7. View cart");
        }
        System.out.println("0. Exit");
    }

    public void generateData() {
        System.out.println("Warning: this operation replaces current business CSV data.");
        String confirmation = input.readLine("Enter CREATE to confirm, or anything else to go back: ");
        if (!"CREATE".equalsIgnoreCase(confirmation)) {
            System.out.println("Current data was left unchanged.");
            return;
        }
        try {
            Map<String, Integer> result = dataController.generateData();
            reportView.displayDataGenerationResult(result);
        } catch (IOException exception) {
            System.out.println("Unable to generate CSV data. Check write permissions for the data directory.");
        }
    }

    public void openAdmin() {
        if (adminCredentials.isEmpty()) {
            System.out.printf("Admin login is disabled. Configure %s and %s (or the legacy %s and %s).%n",
                    SecurityEnvironment.ADMIN_USERNAME,
                    SecurityEnvironment.ADMIN_PASSWORD_HASH,
                    SecurityEnvironment.LEGACY_ADMIN_USERNAME,
                    SecurityEnvironment.LEGACY_ADMIN_PASSWORD_HASH);
            return;
        }
        if (failedAdminAttempts >= MAX_ADMIN_FAILURES) {
            System.out.println("Admin login is locked for this session after too many failed attempts.");
            return;
        }

        String username = input.readStringRequired("Admin username");
        String password = input.readStringRequired("Admin password");
        if (adminCredentials.get().authenticate(username, password)) {
            failedAdminAttempts = 0;
            System.out.println("Admin login successful.");
            adminView.setDataGenerationAction(this::generateData);
            adminView.display();
            return;
        }
        failedAdminAttempts++;
        System.out.printf("Invalid credentials. %d attempts remaining in this session.%n",
                Math.max(0, MAX_ADMIN_FAILURES - failedAdminAttempts));
    }

    public void refreshSession() {
        if (loggedInCustomer == null) {
            return;
        }
        Optional<Customer> current = customerController.findCustomer(loggedInCustomer.getCustomerId());
        if (current.isEmpty()
                || !ACTIVE_STATUS.equalsIgnoreCase(current.get().getStatus().name())) {
            System.out.println("This account is no longer active. You have been logged out.");
            loggedInCustomer = null;
            return;
        }
        loggedInCustomer = current.get();
    }

    public void logout() {
        loggedInCustomer = null;
        System.out.println("Logged out successfully.");
    }
}
