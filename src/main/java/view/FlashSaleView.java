package view;

import model.FlashSaleItem;
import java.util.List;

public class FlashSaleView {

    //hien thi danh sach cac sp flashsale
    public void displayItems(List<FlashSaleItem> items) {
        if (items == null || items.isEmpty()) {
            System.out.println("No Flash Sale products are currently on sale.");
            return;
        }

        System.out.printf("%-15s %-32s %14s %14s %10s%n",
            "Item ID", "Product Name", "Original Price (VND)", "Sale Price (VND)", "Stock");
        System.out.println("-".repeat(90));

        for (FlashSaleItem item : items) {
            System.out.printf("%-15s %-32s %,14.0f %,14.0f %10d%n",
                    item.getId(),
                    trim(item.getProductName(), 32),
                    item.getOriginalPrice().doubleValue(),
                    item.getSalePrice().doubleValue(),
                    item.getStock());
        }
        System.out.println("-".repeat(90));
    }

    //hien thi chi tiet 1 sp flash sale
    public void displayItem(FlashSaleItem item) {
        if (item == null) {
            System.out.println("Flash Sale product information not found.");
            return;
        }
        displayItems(List.of(item));
    }

    //cat gon chuoi van ban dai de khong lam vo khung bang
    private String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}