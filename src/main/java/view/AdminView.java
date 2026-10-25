package view;

import model.Customer;
import model.enums.CustomerStatus;
import repository.CustomerRepository;
import repository.FlashSaleItemRepository;

import java.util.List;

//man hinh admin
//xem ds user, ban/unban
//cau hinh so lg ton kho

public class AdminView {
    private final CustomerRepository customerRepo;
    private final FlashSaleItemRepository itemRepo;
    private final ConsoleInput input;

    public AdminView(CustomerRepository customerRepo, FlashSaleItemRepository itemRepo, ConsoleInput input) {
        this.customerRepo = customerRepo;
        this.itemRepo = itemRepo;
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            int choice = input.readInt("Choose an option", 0);
            switch (choice) {
                case 1 -> listCustomers();
                case 2 -> toggleCustomerBan();
                case 3 -> resetStock();
                case 0 -> back = true;
                default -> System.out.println("Invalid choice. Please try again!");
            }
        }
    }

    public void printMenu() {
        System.out.println("\n===== ADMIN MENU =====");
        System.out.println("1. View customer list");
        System.out.println("2. Ban / Unban Account");
        System.out.println("3. Reset Flash Sale stock");
        System.out.println("0. Back to Main Menu");
        System.out.println("=========================");
    }

    //xem toan bo ds cuctomer

    public void listCustomers() {
        System.out.println("\n-------------------CUSTOMER LIST---------------------");
        System.out.printf("%-10s | %-20s | %-25s | %-10s | %-8s%n",
            "Customer ID", "Full Name", "Email", "VIP Tier", "Status"
        );
        System.out.println("-".repeat(82));

        List<Customer> list = customerRepo.getAll();
        if (list.isEmpty()) {
            System.out.println("No customer data found.");
        } else {
            for (Customer c : list) {
                System.out.printf("%-10s | %-20s | %-25s | %-10s | %-8s%n",
                    c.getCustomerId(),
                    c.getName(),
                    c.getEmail(),
                    c.getTier(),
                    c.getStatus()
                );
            }
        }
        System.out.println("-".repeat(82));
    }

    //ban hoac unban quyen mua hang cuacustomer

    public void toggleCustomerBan() {
        listCustomers();
        String email = input.readStringRequired("Enter account Email to Ban/Unban: ");
        List<Customer> list = customerRepo.getAll();
        Customer target = null;

        for (Customer c : list) {
            if (c.getEmail() != null && c.getEmail().equalsIgnoreCase(email.trim())) {
                target = c;
                break;
            }
        }

        if (target == null) {
            System.out.println(">> No customer found with Email: " + email);
            return;
        }

        //chuyen doi trang thai qua lai giua active va banned
        if (target.getStatus() == CustomerStatus.ACTIVE) {
            target.setStatus(CustomerStatus.BANNED);
            System.out.println(">> BANNED account: " + target.getEmail());
        } else {
            target.setStatus(CustomerStatus.ACTIVE);
            System.out.println(">> UNBANNED account: " + target.getEmail());
        }

        customerRepo.saveAll(list);
    }

    //dieu chinh so luong hang trong kho cho su kien flash slae

    public void resetStock() {
        String itemId = input.readLine("Enter product ID (default ITEM001, press Enter to select): ");
        if (itemId.isBlank()) {
            itemId = "ITEM001";
        }

        int newStock = input.readIntMin("Enter new stock quantity", 0, "Stock cannot be a negative number!");
        itemRepo.resetStock(itemId, newStock);
        System.out.printf(">> Updated stock for [%s] to: %d items.%n", itemId, newStock);
    }
}