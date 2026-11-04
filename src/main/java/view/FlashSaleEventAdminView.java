package view;

import controller.FlashSaleController;
import model.FlashSaleEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Giao diện quản lý vòng đời và CRUD sự kiện; mọi quy tắc đều được thực thi bên dưới lớp View. */
public class FlashSaleEventAdminView {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FlashSaleController controller;
    private final ConsoleInput input;

    public FlashSaleEventAdminView(FlashSaleController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Select an option", 0)) {
                    case 1 -> displayEvents(controller.listEvents());
                    case 2 -> createEvent();
                    case 3 -> updateEvent();
                    case 4 -> startEvent();
                    case 5 -> endEvent();
                    case 6 -> lockEvent();
                    case 7 -> unlockEvent();
                    case 8 -> deleteEvent();
                    case 0 -> back = true;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (exception.OperationCancelledException exception) {
                System.out.println("Event operation cancelled.");
            } catch (Exception exception) {
                System.out.println("Unable to process event: " + exception.getMessage());
            }
        }
    }

    public void printMenu() {
        System.out.println("\n--- FLASH SALE EVENT MANAGEMENT ---");
        System.out.println("1. List events");
        System.out.println("2. Create a 1–2 hour event");
        System.out.println("3. Update an event that has not started");
        System.out.println("4. Start event");
        System.out.println("5. End event");
        System.out.println("6. Temporarily lock event");
        System.out.println("7. Unlock event");
        System.out.println("8. Delete event");
        System.out.println("0. Back");
    }

    public void createEvent() throws Exception {
        String id = input.readStringRequired("Event ID");
        String name = input.readStringRequired("Event name");
        LocalDateTime start = parseDateTime(input.readDateTime("Start time"));
        LocalDateTime end = parseDateTime(input.readDateTime("End time"));
        controller.createEvent(new FlashSaleEvent(id, name, "", start, end,
                model.enums.SaleStatus.UPCOMING));
        System.out.println("Event created.");
    }

    public void updateEvent() throws Exception {
        String id = input.readStringRequired("Event ID to update");
        FlashSaleEvent current = controller.findEvent(id);
        String name = readOrDefault("Name", current.getName());
        LocalDateTime start = readDateOrDefault("Starts", current.getStartTime());
        LocalDateTime end = readDateOrDefault("Ends", current.getEndTime());
        controller.updateEvent(new FlashSaleEvent(id, name, current.getDescription(),
                start, end, current.getStatus()));
        System.out.println("Event updated.");
    }

    public void startEvent() throws Exception {
        controller.startEvent(input.readStringRequired("Event ID to start"));
        System.out.println("Event started.");
    }

    public void endEvent() throws Exception {
        controller.endEvent(input.readStringRequired("Event ID to end"));
        System.out.println("Event ended.");
    }

    public void lockEvent() throws Exception {
        String id = input.readStringRequired("Event ID to lock");
        LocalDateTime unlockAt = LocalDateTime.parse(
                input.readDateTime("Automatic unlock time"), FORMATTER);
        controller.lockEvent(id, unlockAt);
        System.out.println("Event temporarily locked.");
    }

    public void unlockEvent() throws Exception {
        controller.unlockEvent(input.readStringRequired("Event ID to unlock"));
        System.out.println("Event unlocked.");
    }

    public void deleteEvent() throws Exception {
        String id = input.readStringRequired("Event ID to delete");
        if (!"DELETE".equalsIgnoreCase(input.readLine("Enter DELETE to confirm: "))) {
            System.out.println("Deletion cancelled.");
            return;
        }
        System.out.println(controller.deleteEvent(id) ? "Event deleted." : "Deletion failed.");
    }

    public String readOrDefault(String label, String current) {
        String value = input.readLine(label + " [" + current + "]: ");
        return value.isBlank() ? current : value;
    }

    public LocalDateTime readDateOrDefault(String label, LocalDateTime current) {
        String value = input.readLine(label + " [" + current + "] (yyyy-MM-dd HH:mm:ss): ");
        if (value.isBlank()) {
            return current;
        }
        return parseDateTime(value);
    }

    public LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value, FORMATTER);
    }

    public void displayEvents(List<FlashSaleEvent> events) {
        if (events.isEmpty()) {
            System.out.println("No events found.");
            return;
        }
        System.out.printf("%-10s %-25s %-20s %-20s %-12s%n",
                "ID", "Name", "Starts", "Ends", "Status");
        System.out.println("--------------------------------------------------------------------------------------------");
        for (FlashSaleEvent event : events) {
            System.out.printf("%-10s %-25s %-20s %-20s %-12s%n",
                    event.getEventId(), trim(event.getName(), 25), event.getStartTime(),
                    event.getEndTime(), event.getStatus());
        }
    }

    public static String trim(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit - 3) + "...";
    }
}

// Thành viên 3
