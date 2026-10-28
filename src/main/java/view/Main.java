package view;

import controller.CustomerController;
import controller.DataController;
import controller.SimulatorController;
import repository.CustomerRepository;
import repository.FlashSaleItemRepository;
import repository.FlashSaleEventRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.ProductRepository;
import repository.SimulationRecordRepository;
import repository.VoucherRepository;
import service.CustomerService;
import service.DataNormalizationService;
import service.DataGeneratorService;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length > 0 && "--generate-data".equalsIgnoreCase(args[0])) {
            new ReportView().displayDataGenerationResult(
                    new DataController(new DataGeneratorService()).generateData());
            return;
        }
        if (args.length > 0 && "--normalize-data".equalsIgnoreCase(args[0])) {
            System.out.println("Normalized linked data: "
                    + new DataNormalizationService("data").normalizeRelations());
            return;
        }

        Scanner scanner = new Scanner(System.in);
        ConsoleInput input = new ConsoleInput(scanner);

        // 1. Tầng Repository
        CustomerRepository customerRepo = new CustomerRepository("data/customers.csv");
        VoucherRepository voucherRepo = new VoucherRepository("data/vouchers.csv");
        FlashSaleItemRepository itemRepo = new FlashSaleItemRepository("data");
        FlashSaleEventRepository eventRepo = new FlashSaleEventRepository("data");
        ProductRepository productRepo = new ProductRepository("data");
        OrderRepository orderRepo = new OrderRepository("data");
        OrderDetailRepository orderDetailRepo = new OrderDetailRepository("data");
        OrderTransactionRepository transactionRepo = new OrderTransactionRepository("data");

        // 2. Tầng Service & Controller
        CustomerService customerService = new CustomerService(customerRepo, voucherRepo);
        CustomerController customerController = new CustomerController(customerService);
        SimulatorController simulatorController = new SimulatorController(
                itemRepo, new SimulationRecordRepository());

        // 3. Tầng View
        FlashSaleView flashSaleView = new FlashSaleView();
        DataManagementView dataManagementView = new DataManagementView(
                input, customerRepo, productRepo, orderRepo, voucherRepo,
                itemRepo, eventRepo, orderDetailRepo, transactionRepo);
        AdminView adminView = new AdminView(
        new ProductAdminView(productRepo, input),
        new FlashSaleEventAdminView(eventRepo, input),
        new FlashSaleItemAdminView(itemRepo, input),
        new AdminCustomerView(customerRepo, input),
        new AdminVoucherView(voucherRepo, input),
        new AdminOrderView(orderRepo, orderDetailRepo, transactionRepo, input),
        new AdminReportView(productRepo, customerRepo, voucherRepo, eventRepo,
                itemRepo, orderRepo, orderDetailRepo, transactionRepo),
        dataManagementView,
        input
);
        CustomerAccountView accountView = new CustomerAccountView(customerController, input,
                new CustomerVoucherView(customerController, voucherRepo, input));
        SimulatorView simulatorView = new SimulatorView(input, simulatorController);

        // 4. Khởi động Menu Router chính
        MainView mainView = new MainView(
                input, flashSaleView, itemRepo, eventRepo, adminView, accountView,
                customerController, simulatorView);
        try {
            mainView.display();
        } finally {
            simulatorController.shutdownExecutor();
            scanner.close();
        }
    }
}
