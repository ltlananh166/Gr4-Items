package view;

import controller.CustomerController;
import controller.SimulatorController;
import repository.CustomerRepository;
import repository.FlashSaleItemRepository;
import repository.SimulationRecordRepository;
import repository.VoucherRepository;
import service.CustomerService;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ConsoleInput input = new ConsoleInput(scanner);

        // 1. Tầng Repository
        CustomerRepository customerRepo = new CustomerRepository("data/customers.csv");
        VoucherRepository voucherRepo = new VoucherRepository("data/vouchers.csv");
        FlashSaleItemRepository itemRepo = new FlashSaleItemRepository();

        // 2. Tầng Service & Controller
        CustomerService customerService = new CustomerService(customerRepo, voucherRepo);
        CustomerController customerController = new CustomerController(customerService);
        SimulatorController simulatorController = new SimulatorController(
                itemRepo, new SimulationRecordRepository());

        // 3. Tầng View
        FlashSaleView flashSaleView = new FlashSaleView();
        AdminView adminView = new AdminView(
        new ProductAdminView(),
        new FlashSaleEventAdminView(),
        new FlashSaleItemAdminView(),
        new AdminCustomerView(),
        new AdminVoucherView(),
        new AdminOrderView(),
        new AdminReportView(),
        input
);
        CustomerAccountView accountView = new CustomerAccountView(customerController, input);
        SimulatorView simulatorView = new SimulatorView(input, simulatorController);

        // 4. Khởi động Menu Router chính
        MainView mainView = new MainView(
                input, flashSaleView, itemRepo, adminView, accountView,
                customerController, simulatorView);
        try {
            mainView.display();
        } finally {
            simulatorController.shutdownExecutor();
            scanner.close();
        }
    }
}
