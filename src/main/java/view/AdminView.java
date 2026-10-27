package view;


public class AdminView {


    private final ProductAdminView productView;
    private final FlashSaleEventAdminView eventView;
    private final FlashSaleItemAdminView itemView;
    private final AdminCustomerView customerView;
    private final AdminVoucherView voucherView;
    private final AdminOrderView orderView;
    private final AdminReportView reportView;
    private final ConsoleInput input;


    public AdminView(ProductAdminView productView,
                     FlashSaleEventAdminView eventView,
                     FlashSaleItemAdminView itemView,
                     AdminCustomerView customerView,
                     AdminVoucherView voucherView,
                     AdminOrderView orderView,
                     AdminReportView reportView,
                     ConsoleInput input) {
        this.productView = productView;
        this.eventView = eventView;
        this.itemView = itemView;
        this.customerView = customerView;
        this.voucherView = voucherView;
        this.orderView = orderView;
        this.reportView = reportView;
        this.input = input;
    }


    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            int choice = input.readInt("Choose an option", 0);
            switch (choice) {
                case 1 -> productView.display();
                case 2 -> eventView.display();
                case 3 -> itemView.display();
                case 4 -> customerView.display();
                case 5 -> voucherView.display();
                case 6 -> orderView.display();
                case 7 -> reportView.display();
                case 0 -> back = true;
                default -> System.out.println("Invalid choice. Please try again!");
            }
        }
    }


    public void printMenu() {
        System.out.println("\n===== ADMINISTRATOR CONTROL PANEL =====");
        System.out.println("1. Product Management (CRUD + Search)");
        System.out.println("2. Flash Sale Event Management");
        System.out.println("3. Flash Sale Item Management (Stock Allocation)");
        System.out.println("4. Customer Management (List + Ban/Unban)");
        System.out.println("5. Voucher Management (CRUD + Limits)");
        System.out.println("6. Orders, details & transactions");
        System.out.println("7. Data & sales summary");
        System.out.println("0. Back to Main Menu");
        System.out.println("========================================");
    }
}