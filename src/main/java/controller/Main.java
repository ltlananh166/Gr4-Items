package controller;

import java.io.IOException;

/** Điểm khởi chạy tương thích, chuyển tiếp đến giao diện chính của ứng dụng. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {
        view.Main.main(args);
    }
}
