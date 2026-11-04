package view;

import model.FlashSaleItem;

import java.math.RoundingMode;
import java.util.List;

public class FlashSaleView {
    public void displayItems(List<FlashSaleItem> items) {
        if (items.isEmpty()) {
            System.out.println("No Flash Sale items found. Generate the CSV data first.");
            return;
        }

        System.out.printf("%-15s %-32s %12s %12s %10s%n",
                "Product ID (Item ID)", "Product name", "Original price", "Sale price", "Remaining");
        System.out.println("-------------------------------------------------------------------------------");
        for (FlashSaleItem item : items) {
            System.out.printf("%-15s %-32s %,12.0f %,12.0f %10d%n",
                    item.getItemId(),
                    trim(item.getProductName(), 32),
                    item.getOriginalPrice().setScale(0, RoundingMode.HALF_UP),
                    item.getSalePrice().setScale(0, RoundingMode.HALF_UP),
                    item.getRemainingStock());
        }
    }

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

// Thành viên 3
