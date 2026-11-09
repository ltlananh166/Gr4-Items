package view;

import controller.AdminCustomerController;
import controller.AdminOrderController;
import controller.AdminReportController;
import controller.CustomerController;
import controller.DataController;
import controller.FlashSaleController;
import controller.OrderController;
import controller.OrderTrackingController;
import controller.ProductController;
import controller.SimulationReportController;
import controller.SimulatorController;
import controller.VoucherController;
import repository.CartItemRepository;
import repository.CustomerRepository;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.ProductRepository;
import repository.VoucherRepository;
import security.AdminCredentials;
import service.AdminOrderService;
import service.CartService;
import service.AdminReportService;
import service.CustomerAdminService;
import service.DataGeneratorService;
import service.DataNormalizationService;
import service.FlashSaleService;
import service.FlashSaleServiceImpl;
import service.SimulationReportService;
import service.SimulatorService;
import service.VoucherService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        DataController dataController = new DataController(new DataGeneratorService());
        if (args.length > 0 && "--generate-data".equalsIgnoreCase(args[0])) {
            new ReportView().displayDataGenerationResult(dataController.generateData());
            return;
        }
        if (args.length > 0 && "--normalize-data".equalsIgnoreCase(args[0])) {
            System.out.println("Normalized linked data: "
                    + new DataNormalizationService("data").normalizeRelations());
            return;
        }

        Path dataDirectory = Path.of("data");
        Scanner scanner = new Scanner(System.in);
        try {
            ConsoleInput input = new ConsoleInput(scanner);
            CustomerRepository customerRepository = new CustomerRepository(
                    dataDirectory.resolve("customers.csv").toString());
            VoucherRepository voucherRepository = new VoucherRepository(
                    dataDirectory.resolve("vouchers.csv").toString());
            FlashSaleItemRepository itemRepository =
                    new FlashSaleItemRepository(dataDirectory.toString());
            FlashSaleEventRepository eventRepository =
                    new FlashSaleEventRepository(dataDirectory.toString());
            OrderRepository orderRepository = new OrderRepository(dataDirectory.toString());
            OrderDetailRepository detailRepository =
                    new OrderDetailRepository(dataDirectory.toString());
            OrderTransactionRepository transactionRepository =
                    new OrderTransactionRepository(dataDirectory.toString());
            ProductRepository productRepository = new ProductRepository(dataDirectory.toString());

            FlashSaleService flashSaleService = new FlashSaleServiceImpl(
                    itemRepository, orderRepository, detailRepository, customerRepository,
                    voucherRepository, transactionRepository, eventRepository);
            CartItemRepository cartItemRepository =
                    new CartItemRepository(dataDirectory.toString());
            CartService cartService = new CartService(
                    cartItemRepository, itemRepository, eventRepository, flashSaleService);
            CustomerController customerController = new CustomerController(customerRepository);
            FlashSaleController flashSaleController =
                    new FlashSaleController(itemRepository, eventRepository);
            OrderController orderController = new OrderController(flashSaleService, cartService);
            ProductController productController = new ProductController(productRepository);
            VoucherController voucherController =
                    new VoucherController(new VoucherService(voucherRepository));
            OrderTrackingController trackingController = new OrderTrackingController(
                    orderRepository, detailRepository, transactionRepository,
                    productRepository, itemRepository);

            FlashSaleView flashSaleView = new FlashSaleView();
            OrderView orderView = new OrderView();
            SimulatorController simulatorController =
                    new SimulatorController(new SimulatorService(transactionRepository));

            AdminView adminView = new AdminView(
                    new ProductAdminView(productController, input),
                    new FlashSaleEventAdminView(flashSaleController, input),
                    new FlashSaleItemAdminView(
                            flashSaleController, productController, flashSaleView, input),
                    new AdminCustomerView(new AdminCustomerController(
                            new CustomerAdminService(
                                    customerRepository,
                                    flashSaleService::cancelAllPendingAndApprovedOrders)),
                            input),
                    new AdminVoucherView(voucherController, input),
                    new AdminOrderView(new AdminOrderController(
                            new AdminOrderService(orderRepository, detailRepository,
                                    productRepository),
                            orderController),
                            input),
                    new AdminReportView(new AdminReportController(new AdminReportService(
                            orderRepository, customerRepository,
                            voucherRepository, transactionRepository))),
                    input);

            MainView mainView = new MainView(
                    customerController,
                    dataController,
                    new CustomerAccountView(customerController, input),
                    new FlashSaleShoppingView(
                            flashSaleController, orderController, flashSaleView, orderView, input),
                    new CartView(orderController, input),
                    new OrderTrackingView(trackingController, input),
                    new CustomerVoucherView(voucherController),
                    adminView,
                    new ResearcherView(simulatorController,
                            new SimulationReportController(new SimulationReportService()),
                            input),
                    new ReportView(),
                    AdminCredentials.fromEnvironment(),
                    input);
            mainView.display();
        } finally {
            scanner.close();
        }
    }
}
