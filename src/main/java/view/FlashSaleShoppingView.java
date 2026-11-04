package view;

import controller.FlashSaleController;
import controller.OrderController;
import exception.EndOfInputException;
import exception.OperationCancelledException;
import model.Customer;
import model.FlashSaleItem;
import model.FlashSaleEvent;
import service.FlashSaleConstants;

import java.util.List;

/** Giao diện dòng lệnh dành cho khách hàng để xem và đặt hàng Flash Sale. */
public class FlashSaleShoppingView {
    private static final int MIN_QUANTITY = 1;

    private final FlashSaleController flashSaleController;
    private final OrderController orderController;
    private final FlashSaleView itemTable;
    private final OrderView orderView;
    private final ConsoleInput input;

    public FlashSaleShoppingView(FlashSaleController flashSaleController,
                                 OrderController orderController,
                                 FlashSaleView itemTable,
                                 OrderView orderView,
                                 ConsoleInput input) {
        this.flashSaleController = flashSaleController;
        this.orderController = orderController;
        this.itemTable = itemTable;
        this.orderView = orderView;
        this.input = input;
    }

    public void browse() {
        browse(null);
    }

    public void browse(Customer customer) {
        List<FlashSaleEvent> events = flashSaleController.listCustomerVisibleEvents();
        if (events.isEmpty()) {
            System.out.println("There are no active Flash Sale events.");
            return;
        }

        printEvents(events);
        FlashSaleEvent selected = selectEvent(events);
        if (selected == null) {
            System.out.println("The selected event could not be found.");
            return;
        }

        System.out.printf("%n--- PRODUCTS: %s (%s) ---%n",
                selected.getName(), selected.getEventId());
        itemTable.displayItems(flashSaleController.getItemsByEvent(selected.getEventId()));
        addItemToCart(customer);
    }

    public boolean book(Customer customer) {
        if (customer == null) {
            System.out.println("Please log in before placing an order.");
            return false;
        }

        try {
            String itemId = input.readStringRequired("Flash Sale item ID");
            FlashSaleItem item = flashSaleController.findFlashSaleItem(itemId);
            if (item == null) {
                System.out.println("Flash Sale item not found.");
                return false;
            }

            System.out.printf("%s | Sale price: %,.0f VND | Remaining stock: %d%n",
                    item.getProductName(), item.getSalePrice(), item.getRemainingStock());
            int quantity = input.readIntMinMax("Quantity", MIN_QUANTITY,
                    FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT,
                    "Each customer may buy 1 to "
                            + FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT
                            + " items per event.");
            String voucherCode = input.readLine("Voucher code (leave blank to skip): ");
            String normalizedVoucher = voucherCode.isBlank() ? null : voucherCode;

            boolean success = orderController.bookItem(
                    itemId, quantity, customer.getCustomerId(), normalizedVoucher);
            orderView.displayBookingResult(success);
            return success;
        } catch (exception.OperationCancelledException exception) {
            System.out.println("Order placement cancelled.");
            return false;
        } catch (Exception exception) {
            orderView.displayBookingError(exception);
            return false;
        }
    }

    public void addItemToCart(Customer customer) {
        String confirmation = input.readLine("Add a Flash Sale item to your cart? (Y/N): ");
        if (!"Y".equalsIgnoreCase(confirmation)) {
            return;
        }
        if (customer == null) {
            System.out.println(
                    "Please log in or register an account to add items to and view your cart.");
            return;
        }
        try {
            String itemId = input.readStringRequired("Flash Sale item ID");
            int quantity = input.readIntMinMax("Quantity", MIN_QUANTITY,
                    FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT,
                    "Each customer may add 1 to "
                            + FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT
                            + " items per product.");
            orderController.addCartItem(customer.getCustomerId(), itemId, quantity);
            System.out.println("Item added to your cart. Inventory has not been reserved.");
        } catch (EndOfInputException | OperationCancelledException exception) {
            System.out.println("Add to cart cancelled.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            System.out.println("Unable to add item: " + exception.getMessage());
        }
    }

    public void printEvents(List<FlashSaleEvent> events) {
        System.out.println("\n--- AVAILABLE FLASH SALE EVENTS ---");
        System.out.printf("%-4s | %-14s | %-28s | %-19s | %-19s | %-10s%n",
                "No.", "ID", "Name", "Starts", "Ends", "Status");
        System.out.println("-".repeat(108));
        for (int index = 0; index < events.size(); index++) {
            FlashSaleEvent event = events.get(index);
            System.out.printf("%-4d | %-14s | %-28s | %-19s | %-19s | %-10s%n",
                    index + 1, event.getEventId(), trim(event.getName(), 28),
                    event.getStartTime(), event.getEndTime(), event.getStatus());
        }
    }

    public FlashSaleEvent selectEvent(List<FlashSaleEvent> events) {
        String selection = input.readStringRequired("Enter the number or event ID");
        try {
            int index = Integer.parseInt(selection);
            if (index >= 1 && index <= events.size()) {
                return events.get(index - 1);
            }
        } catch (NumberFormatException ignored) {
            // Giá trị nhập tương tự cũng có thể là mã sự kiện.
        }
        return events.stream()
                .filter(event -> event.getEventId().equalsIgnoreCase(selection))
                .findFirst()
                .orElse(null);
    }

    public static String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}

// Thành viên 3
