
package view;

import java.util.Scanner;

public class Main {

    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        Main app = new Main();
        app.run();
    }

    public void run() {
        boolean running = true;

        while (running) {
            showMainMenu();

            int choice = readInt("Choose an option: ");

            switch (choice) {
                case 1:
                    registerMenu();
                    break;

                case 2:
                    loginMenu();
                    break;

                case 3:
                    viewProductsMenu();
                    break;

                case 4:
                    flashSaleMenu();
                    break;

                case 5:
                    voucherMenu();
                    break;

                case 6:
                    transactionHistoryMenu();
                    break;

                case 0:
                    running = false;
                    System.out.println("Thank you for using Flash Sale System!");
                    break;

                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }

        SCANNER.close();
    }

    private void showMainMenu() {
        System.out.println("\n========================================");
        System.out.println("       E-COMMERCE FLASH SALE SYSTEM");
        System.out.println("========================================");
        System.out.println("1. Register Account");
        System.out.println("2. Login");
        System.out.println("3. View Products");
        System.out.println("4. Flash Sale Purchase");
        System.out.println("5. Apply Voucher");
        System.out.println("6. Transaction History");
        System.out.println("0. Exit");
        System.out.println("========================================");
    }

    private void registerMenu() {
        System.out.println("\n--- REGISTER ACCOUNT ---");
        System.out.println("Register function is waiting for CustomerController.");
    }

    private void loginMenu() {
        System.out.println("\n--- LOGIN ---");
        System.out.println("Login function is waiting for CustomerController.");
    }

    private void viewProductsMenu() {
        System.out.println("\n--- PRODUCT LIST ---");
        System.out.println("Product listing is waiting for the Product module.");
    }

    private void flashSaleMenu() {
        System.out.println("\n--- FLASH SALE PURCHASE ---");
        System.out.println("Purchase function is waiting for the Order module.");
    }

    private void voucherMenu() {
        System.out.println("\n--- VOUCHER ---");
        System.out.println("Voucher function is waiting for the Voucher module.");
    }

    private void transactionHistoryMenu() {
        System.out.println("\n--- TRANSACTION HISTORY ---");
        System.out.println("History function is waiting for the Transaction module.");
    }

    private int readInt(String message) {
        while (true) {
            System.out.print(message);

            String input = SCANNER.nextLine().trim();

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }
}
