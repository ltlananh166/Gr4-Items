package view;

import controller.AdminCustomerController;
import model.Customer;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Console presentation for administrator customer management.
 * Handled by Member 5 (TV5: View).
 * ONLY handles presentation and delegates all business actions to AdminCustomerController.
 */
public class AdminCustomerView {
    // Chỉ giữ kết nối đến Controller của TV4 và bộ đọc phím
    private final AdminCustomerController controller;
    private final ConsoleInput input;

    public AdminCustomerView(AdminCustomerController controller) {
        this(controller, new ConsoleInput(new Scanner(System.in)));
    }

    public AdminCustomerView(AdminCustomerController controller, ConsoleInput input) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.input = Objects.requireNonNull(input, "input");
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            int choice = input.readInt("Enter your choice", 0);
            switch (choice) {
                case 1 -> showCustomers(controller.list()); // Gọi Controller
                case 2 -> search();                         // Gọi Controller
                case 3 -> create();                         // Gọi Controller
                case 4 -> update();                         // Gọi Controller
                case 5 -> delete();                         // Gọi Controller
                case 6 -> changeStatus("BANNED");           // Gọi Controller
                case 7 -> changeStatus("ACTIVE");           // Gọi Controller
                case 0 -> back = true;
                default -> System.out.println("Invalid choice. Please try again!");
            }
        }
    }

    public void printMenu() {
        System.out.println("\n===== CUSTOMER ACCOUNT MANAGEMENT =====");
        System.out.println("1. View customer list");
        System.out.println("2. Search customer (ID, Name, Email, Phone)");
        System.out.println("3. Add new customer");
        System.out.println("4. Update customer information");
        System.out.println("5. Delete customer");
        System.out.println("6. Ban account (Lock)");
        System.out.println("7. Unban account (Unlock)");
        System.out.println("0. Back to Admin Menu");
        System.out.println("========================================");
    }

    // 1. Tìm kiếm: Nhập từ khóa -> đẩy sang Controller xử lý
    private void search() {
        String keyword = input.readStringRequired("Enter ID, name, email or phone number to search: ");
        showCustomers(controller.search(keyword));
    }

    // 2. Thêm mới: Hỏi thông tin -> đẩy sang Controller tạo
    private void create() {
        System.out.println("\n--- ADD NEW CUSTOMER ---");
        String name = input.readStringRequired("Full name: ");
        String email = input.readStringRequired("Email: ");
        String phone = input.readStringRequired("Phone number: ");
        String address = input.readStringRequired("Address: ");
        String avatarUrl = input.readLine("Avatar URL (optional): ");
        String tier = input.readLine("Membership tier (STANDARD/SILVER/GOLD/DIAMOND) [STANDARD]: ");
        String password = input.readStringRequired("Password (min 8 characters): ");

        printAction(controller.create(name, email, phone, address, avatarUrl, tier, password));
    }

    // 3. Cập nhật: Nhập ID và thông tin mới -> đẩy sang Controller cập nhật
    private void update() {
        System.out.println("\n--- UPDATE CUSTOMER INFORMATION ---");
        String customerId = input.readStringRequired("Enter customer ID to update (e.g. C-00001): ");
        System.out.println("Enter new details (leave blank and press Enter to keep current value):");
        String name = input.readLine("New full name: ");
        String email = input.readLine("New email: ");
        String phone = input.readLine("New phone: ");
        String address = input.readLine("New address: ");
        String avatarUrl = input.readLine("New avatar URL: ");
        String tier = input.readLine("New tier (STANDARD/SILVER/GOLD/DIAMOND): ");
        String password = input.readLine("New password: ");

        printAction(controller.update(customerId, name, email, phone, address, avatarUrl, tier, password));
    }

    // 4. Xóa: Xác nhận Y/N -> đẩy sang Controller xóa
    private void delete() {
        System.out.println("\n--- DELETE CUSTOMER ---");
        String customerId = input.readStringRequired("Enter customer ID to delete: ");
        String confirmation = input.readLine("Are you sure you want to delete customer " + customerId + "? (Y/N): ");
        if (!"Y".equalsIgnoreCase(confirmation != null ? confirmation.trim() : "")) {
            System.out.println(">> Delete operation cancelled.");
            return;
        }
        printAction(controller.delete(customerId));
    }

    // 5. Khóa / Mở khóa -> đẩy sang Controller đổi trạng thái
    private void changeStatus(String status) {
        String actionName = "BANNED".equalsIgnoreCase(status) ? "BAN" : "UNBAN";
        System.out.println("\n--- " + actionName + " ACCOUNT ---");
        String customerId = input.readStringRequired("Enter customer ID: ");
        printAction(controller.setStatus(customerId, status));
    }

    // In bảng dữ liệu mà Controller trả về
    private void showCustomers(AdminCustomerController.Result<List<Customer>> result) {
        if (!result.success()) {
            System.out.println(">> Error: " + result.message());
            return;
        }
        List<Customer> customers = result.data();
        if (customers == null || customers.isEmpty()) {
            System.out.println("\n>> No customers found in the list.");
            return;
        }
        System.out.println("\n" + "=".repeat(110));
        System.out.printf("%-10s | %-20s | %-28s | %-12s | %-10s | %-10s | %-12s%n",
                "ID", "Full Name", "Email", "Phone", "Tier", "Status", "Wallet (VND)");
        System.out.println("-".repeat(110));
        for (Customer c : customers) {
            System.out.printf("%-10s | %-20s | %-28s | %-12s | %-10s | %-10s | %,12.0f%n",
                    text(c.getCustomerId(), 10),
                    text(c.getName(), 20),
                    text(c.getEmail(), 28),
                    text(c.getPhone(), 12),
                    c.getTier() != null ? c.getTier().name() : "STANDARD",
                    c.getStatus() != null ? c.getStatus().name() : "ACTIVE",
                    c.getWalletBalance());
        }
        System.out.println("=".repeat(110));
    }

    // In thông báo Thành công / Lỗi mà Controller trả về
    private void printAction(AdminCustomerController.Result<?> result) {
        System.out.println((result.success() ? ">> Success: " : ">> Error: ") + result.message());
    }

    private static String text(String value, int width) {
        String safe = value == null ? "" : value;
        return safe.length() <= width ? safe : safe.substring(0, width - 3) + "...";
    }
}