package view;

import model.FlashSaleItem;
import java.util.List;

public class FlashSaleView {

    // 1. Hiển thị danh sách các sản phẩm flash sale (+)
    public void displayItems(List<FlashSaleItem> items) {
        if (items == null || items.isEmpty()) {
            System.out.println("No Flash Sale products are currently on sale.");
            return;
        }

        System.out.printf("%-10s %-20s %20s %16s %8s%n",
            "Item ID", "Product Name", "Original Price (VND)", "Sale Price (VND)", "Stock");
        System.out.println("-".repeat(83));

        for (FlashSaleItem item : items) {
            System.out.printf("%-10s %-20s %,20.0f %,16.0f %8d%n",
                    item.getId(),
                    trim(item.getProductName(), 20),
                    item.getOriginalPrice().doubleValue(),
                    item.getSalePrice().doubleValue(),
                    item.getStock());
        }
        System.out.println("-".repeat(83));
    }

    // 2. Cắt gọn chuỗi văn bản dài để không làm vỡ khung bảng
    public String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}