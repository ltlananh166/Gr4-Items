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