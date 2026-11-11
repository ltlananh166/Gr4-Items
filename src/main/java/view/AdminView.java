package view;

/** Bộ định tuyến cấp cao nhất của quản trị viên. Lớp này không sở hữu kho dữ liệu hay quy tắc nghiệp vụ. */
public class AdminView {
    private final ProductAdminView productView;
    private final FlashSaleEventAdminView eventView;
    private final FlashSaleItemAdminView itemView;
    private final AdminCustomerView customerView;
    private final AdminVoucherView voucherView;
    private final AdminOrderView orderView;
    private final AdminReportView reportView;
    private final ConsoleInput input;
    private Runnable dataGenerationAction;

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
            try {
                switch (input.readInt("Select an option", 0)) {
                    case 1 -> productView.display();
                    case 2 -> eventView.display();
                    case 3 -> itemView.display();
                    case 4 -> customerView.display();
                    case 5 -> voucherView.display();
                    case 6 -> orderView.display();
                    case 7 -> reportView.display();
                    case 8 -> {
                        if (dataGenerationAction != null) {
                            dataGenerationAction.run();
                        } else {
                            System.out.println("Data generation is not available.");
                        }
                    }
                    case 0 -> back = true;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (exception.OperationCancelledException exception) {
                System.out.println("Admin operation cancelled.");
            }
        }
    }

    void setDataGenerationAction(Runnable dataGenerationAction) {
        this.dataGenerationAction = dataGenerationAction;
    }

    public void printMenu() {
        System.out.println("\n===== ADMIN MENU =====");
        System.out.println("1. Product CRUD + search by category/price range");
        System.out.println("2. Flash Sale event CRUD + lifecycle");
        System.out.println("3. Flash Sale Item CRUD");
        System.out.println("4. Customer CRUD + ban/unban");
        System.out.println("5. Voucher CRUD");
        System.out.println("6. Review and manage orders");
        System.out.println("7. Revenue/tier/voucher/transaction reports");
        if (dataGenerationAction != null) {
            System.out.println("8. Regenerate sample CSV data");
        }
        System.out.println("0. Back");
    }
}
