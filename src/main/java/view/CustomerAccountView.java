package view;

import controller.CustomerController;
import exception.OperationCancelledException;
import model.Customer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class CustomerAccountView {

    private final CustomerController controller;
    private final ConsoleInput input;
    private final CustomerVoucherView voucherView;

    public CustomerAccountView(CustomerController controller, ConsoleInput input) {
        this(controller, input, null);
    }

    public CustomerAccountView(CustomerController controller, ConsoleInput input,
                               CustomerVoucherView voucherView) {
        this.controller = controller;
        this.input = input;
        this.voucherView = voucherView;
    }

    /**
     * Chức năng 1: Đăng ký tài khoản khách hàng mới.
     * Thu thập thông tin -> Đóng gói vào Map payload -> Gọi controller.register(payload).
     */
    public void register() {
        System.out.println("\n--- CUSTOMER REGISTRATION ---");
        try {
            // Bước 1: Nhận thông tin đăng ký từ bàn phím
            String name = input.readStringRequired("Full name: ");
            String email = input.readStringPattern("Email: ",
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", "Invalid email format!");
            String phone = input.readStringPattern("Phone number (10 digits): ",
                    "^0\\d{9}$", "Phone number must start with 0 and contain exactly 10 digits!");
            String address = input.readStringRequired("Address: ");
            String password = input.readStringPattern("Password (min 8 characters): ",
                    "^.{8,}$", "Password must contain at least 8 characters!");

            // Bước 2: Đóng gói thông tin vào Map theo đúng tham số mà CustomerController yêu cầu
            Map<String, String> payload = new HashMap<>();
            payload.put("name", name);
            payload.put("email", email);
            payload.put("phone", phone);
            payload.put("address", address);
            payload.put("password", password);

            // Bước 3: Gọi Controller thực hiện kiểm tra trùng lặp, mã hóa mật khẩu và lưu file
            Map<String, Object> result = controller.register(payload);
            System.out.println(">> " + result.get("message"));

        } catch (OperationCancelledException exception) {
            // Khi người dùng gõ "cancel" tại bất kỳ ô nhập liệu nào
            System.out.println(">> Registration cancelled.");
        } catch (Exception exception) {
            System.out.println(">> Registration error: " + exception.getMessage());
        }
    }

    /**
     * Chức năng 2: Đăng nhập tài khoản khách hàng.
     * Gọi controller.login(payload) và nhận về Optional<Customer>.
     */
    public Optional<Customer> login() {
        System.out.println("\n--- CUSTOMER LOGIN ---");
        try {
            // Bước 1: Đọc thông tin đăng nhập
            String email = input.readStringRequired("Email: ");
            String password = input.readStringRequired("Password: ");

            // Bước 2: Gửi sang Controller để đối chiếu mật khẩu và trạng thái tài khoản
            Map<String, String> payload = Map.of("email", email, "password", password);
            Optional<Customer> loggedIn = controller.login(payload);

            // Bước 3: Phản hồi kết quả cho người dùng
            if (loggedIn.isPresent()) {
                System.out.println(">> Login successful! Welcome, " + loggedIn.get().getName() + ".");
                return loggedIn;
            } else {
                System.out.println(">> Login failed: Invalid email, password, or account is suspended.");
                return Optional.empty();
            }

        } catch (OperationCancelledException exception) {
            System.out.println(">> Login cancelled.");
            return Optional.empty();
        } catch (Exception exception) {
            System.out.println(">> Login error: " + exception.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Chức năng 3: Menu trang quản lý tài khoản dành cho khách hàng đã đăng nhập.
     */
    public Customer displayAccountMenu(Customer customer) {
        Customer current = customer;
        boolean back = false;
        while (!back) {
            printAccountMenu();
            int choice = input.readInt("Choose an option", 0);
            switch (choice) {
                case 1 -> displayProfile(current);
                case 2 -> current = updateProfile(current);
                case 3 -> changePassword(current);
                case 4 -> current = topUpWallet(current);
                case 5 -> {
                    if (voucherView == null) {
                        System.out.println(">> Voucher feature is currently unavailable.");
                    } else {
                        voucherView.display();
                    }
                }
                case 0 -> back = true;
                default -> System.out.println(">> Invalid choice. Please try again!");
            }
        }
        return current;
    }

    /**
     * In danh sách các chức năng trong menu tài khoản.
     */
    public void printAccountMenu() {
        System.out.println("\n===== MY ACCOUNT PROFILE =====");
        System.out.println("1. View Profile");
        System.out.println("2. Update Profile");
        System.out.println("3. Change Password");
        System.out.println("4. Top-up Wallet Balance");
        System.out.println("5. View / Apply Voucher");
        System.out.println("0. Back to Main Menu");
        System.out.println("==============================");
    }

    /**
     * Hiển thị chi tiết hồ sơ cá nhân của khách hàng.
     */
    public void displayProfile(Customer customer) {
        System.out.println("\n--- PERSONAL PROFILE ---");
        System.out.println("Customer ID:    " + customer.getCustomerId());
        System.out.println("Full Name:      " + customer.getName());
        System.out.println("Email:          " + customer.getEmail());
        System.out.println("Phone:          " + customer.getPhone());
        System.out.println("Address:        " + customer.getAddress());
        System.out.println("VIP Tier:       " + customer.getTier());
        System.out.println("Account Status: " + customer.getStatus());
        System.out.printf("Wallet Balance: %,.0f VND%n", customer.getWalletBalance());
        System.out.println("------------------------");
    }

    /**
     * Cập nhật thông tin cá nhân:
     * View nhận dữ liệu -> Gửi controller.updateProfile() -> Nhận lại đối tượng Customer mới.
     */
    public Customer updateProfile(Customer customer) {
        System.out.println("\n--- UPDATE PROFILE ---");
        try {
            String name = input.readStringRequired("New full name: ");
            String phone = input.readStringPattern("New phone number (10 digits): ",
                    "^0\\d{9}$", "Phone number must start with 0 and contain exactly 10 digits!");
            String address = input.readStringRequired("New address: ");

            Map<String, String> payload = new HashMap<>();
            payload.put("name", name);
            payload.put("phone", phone);
            payload.put("address", address);

            // Bàn giao cho Controller thực hiện cập nhật và ghi đè file CSV
            Map<String, Object> result = controller.updateProfile(customer.getCustomerId(), payload);
            System.out.println(">> " + result.get("message"));

            // Nếu cập nhật thành công, Controller trả về đối tượng Customer mới đã được cập nhật
            if (Boolean.TRUE.equals(result.get("success")) && result.containsKey("customer")) {
                return (Customer) result.get("customer");
            }
        } catch (OperationCancelledException e) {
            System.out.println(">> Profile update cancelled.");
        }
        return customer;
    }

    /**
     * Đổi mật khẩu:
     * View nhận mật khẩu cũ/mới -> Controller băm mật khẩu và lưu vào DB.
     */
    public void changePassword(Customer customer) {
        System.out.println("\n--- CHANGE PASSWORD ---");
        try {
            String currentPass = input.readStringRequired("Current password: ");
            String newPass = input.readStringPattern("New password (min 8 characters): ",
                    "^.{8,}$", "New password must contain at least 8 characters!");
            String confirmPass = input.readStringRequired("Confirm new password: ");

            // Kiểm tra xác nhận mật khẩu ngay tại tầng View trước khi gọi xuống Controller
            if (!newPass.equals(confirmPass)) {
                System.out.println(">> Error: Password confirmation does not match!");
                return;
            }

            // Gọi Controller kiểm tra mật khẩu cũ và lưu mật khẩu mới
            Map<String, Object> result = controller.changePassword(customer.getCustomerId(), currentPass, newPass);
            System.out.println(">> " + result.get("message"));

        } catch (OperationCancelledException e) {
            System.out.println(">> Password change cancelled.");
        }
    }

    /**
     * Nạp tiền vào ví:
     * View nhận số tiền -> Controller cộng tiền vào tài khoản và cập nhật số dư mới.
     */
    public Customer topUpWallet(Customer customer) {
        System.out.println("\n--- TOP-UP WALLET ---");
        try {
            double amount = input.readDouble("Enter top-up amount: ", 500000);
            if (amount <= 0) {
                System.out.println(">> Error: Top-up amount must be greater than 0!");
                return customer;
            }

            // Gọi Controller xử lý cộng tiền ví và lưu file
            Map<String, Object> result = controller.topUpWallet(customer.getCustomerId(), amount);
            System.out.println(">> " + result.get("message"));

            // Lấy đối tượng Customer với số dư ví mới nhất
            if (Boolean.TRUE.equals(result.get("success")) && result.containsKey("customer")) {
                return (Customer) result.get("customer");
            }
        } catch (OperationCancelledException e) {
            System.out.println(">> Top-up cancelled.");
        }
        return customer;
    }
}