package view;

import java.util.Scanner;

public class MainView {
    private final ConsoleInput input;

    public MainView(ConsoleInput input) {
        this.input = input;
    }

    public void display() {
        boolean isRunning = true;
        while (isRunning) {
            System.out.println("\n=========== HỆ THÔNG BÁN HÀNG FLASH SALE ============");
            System.out.println("1. Dành cho khách vãng lai (Xem sản phẩm)");
            System.out.println("2. Đăng nhập / Đăng ký (khách hàng)");
            System.out.println("3. Đăng nhập quản trị viên (Admin)");
            System.out.println("0. Thoát chương trình");
            System.out.println("=======================================================");

            int choice = input.readIntMinMax("Vui lòng chọn chức năng (0->3): ", 0, 3, "Chỉ được nhập từ 0 đến 3!");

            switch (choice) {
                case 1:
                    System.out.println("--> Đang mở màn hình duyệt sản phẩm...");
                    //gọi flashSaleShoppingView.browse();
                    break;
                case 2:
                    System.out.println("--> Đang mở màn hình tài khoản...");
                    //gọi customerAccountView.display();
                    break;
                case 3:
                    System.out.println("-->Đang mở màn hình Admin...");
                    //gọi adminView.display();
                    break;
                case 0:
                    System.out.println("Cảm ơn bạn đã sử dụng hệ thống! Tạm biệt!");
                    isRunning = false;
                    break;
            }
        }
    }
}