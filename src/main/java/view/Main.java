package view;

import repository.CustomerRepository;
import repository.FlashSaleItemRepository;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ConsoleInput input = new ConsoleInput(scanner);

        // 1. Khởi tạo tầng Repository
        CustomerRepository customerRepo = new CustomerRepository("data/customers.csv");
        FlashSaleItemRepository itemRepo = new FlashSaleItemRepository();

        // 2. Khởi tạo các View đã có
        FlashSaleView flashSaleView = new FlashSaleView();
        AdminView adminView = new AdminView(customerRepo, itemRepo, input);

        // 3. Khởi tạo và chạy MainView
        MainView mainView = new MainView(input, flashSaleView, itemRepo, adminView);
        mainView.display();

        scanner.close();
    }
}