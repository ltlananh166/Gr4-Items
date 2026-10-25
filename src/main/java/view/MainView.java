package view;

import model.FlashSaleItem;
import repository.FlashSaleItemRepository;

import java.util.List;

public class MainView {
    private final ConsoleInput input;
    private final FlashSaleView flashSaleView;
    private final FlashSaleItemRepository itemRepo;
    private final AdminView adminView;

    public MainView(ConsoleInput input, FlashSaleView flashSaleView,
                    FlashSaleItemRepository itemRepo, AdminView adminView) {
        this.input = input;
        this.flashSaleView = flashSaleView;
        this.itemRepo = itemRepo;
        this.adminView = adminView;
    }

    public void display() {
        boolean isRunning = true;
        while (isRunning) {
            System.out.println("\n================ FLASH SALE SYSTEM =================");
            System.out.println("1. Guest access (Browse Flash Sale products)");
            System.out.println("2. Login / Register (Customer)");
            System.out.println("3. Admin Login");
            System.out.println("0. Exit");
            System.out.println("=====================================================");

            int choice = input.readIntMinMax("Please choose an option (0->3): ", 0, 3, "Please enter a number between 0 and 3!");

            switch (choice) {
                case 1 -> browseProducts();
                case 2 -> {
                    System.out.println("--> Opening account screen...");
                    System.out.println("(Customer Account function is waiting for CustomerAccountView)");
                }
                case 3 -> openAdmin();
                case 0 -> {
                    System.out.println("Thank you for using the system! Goodbye!");
                    isRunning = false;
                }
            }
        }
    }

    /**
     * Chức năng 1: Hiển thị sản phẩm Flash Sale bằng FlashSaleView
     */
    private void browseProducts() {
        System.out.println("\n--- CURRENT FLASH SALE PRODUCTS ---");
        FlashSaleItem item = itemRepo.finndById("ITEM001");
        if (item != null) {
            flashSaleView.displayItems(List.of(item));
        } else {
            System.out.println("No products available at the moment.");
        }
    }

    /**
     * Chức năng 3: Đăng nhập quản trị viên và mở AdminView
     */
    private void openAdmin() {
        System.out.println("\n--- ADMIN AUTHENTICATION ---");
        String username = input.readStringRequired("Admin username: ");
        String password = input.readStringRequired("Admin password: ");

        // Tài khoản mặc định: admin / admin123
        if ("admin".equals(username) && "admin123".equals(password)) {
            System.out.println(">> Login successful! Welcome Administrator.");
            adminView.display();
        } else {
            System.out.println(">> Invalid admin credentials! Access denied.");
        }
    }
}