package view;

import model.FlashSaleEvent;
import model.enums.SaleStatus;
import repository.FlashSaleEventRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class FlashSaleEventAdminView {
    private static final DateTimeFormatter INPUT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FlashSaleEventRepository events;
    private final ConsoleInput input;

    public FlashSaleEventAdminView(FlashSaleEventRepository events, ConsoleInput input) {
        this.events = Objects.requireNonNull(events, "events");
        this.input = Objects.requireNonNull(input, "input");
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n===== FLASH SALE EVENTS =====");
            System.out.println("1. List events");
            System.out.println("2. Add event");
            System.out.println("3. Change event status");
            System.out.println("0. Back");
            int choice = input.readIntMinMax("Choose an option", 0, 3,
                    "Choose a number from 0 to 3.");
            switch (choice) {
                case 1 -> listEvents();
                case 2 -> addEvent();
                case 3 -> updateStatus();
                case 0 -> back = true;
                default -> throw new IllegalStateException("Validated event menu choice was out of range.");
            }
        }
    }

    private void listEvents() {
        System.out.printf("%-10s %-24s %-20s %-20s %-12s%n",
                "ID", "Name", "Start", "End", "Status");
        for (FlashSaleEvent event : events.findAll()) {
            System.out.printf("%-10s %-24s %-20s %-20s %-12s%n",
                    event.getEventId(), event.getName(), event.getStartTime(),
                    event.getEndTime(), event.getStatus());
        }
    }

    private void addEvent() {
        String eventId = events.generateNewEventId();
        String name = input.readStringRequired("Event name: ");
        LocalDateTime start = LocalDateTime.parse(
                input.readDateTime("Start time"), INPUT_FORMAT);
        LocalDateTime end = LocalDateTime.parse(
                input.readDateTime("End time"), INPUT_FORMAT);
        FlashSaleEvent event = new FlashSaleEvent(eventId, name, "",
                start, end, SaleStatus.UPCOMING);
        events.save(event);
        System.out.println("Created event " + eventId + ".");
    }

    private void updateStatus() {
        String eventId = input.readStringRequired("Event ID: ");
        FlashSaleEvent event = events.findById(eventId);
        if (event == null) {
            System.out.println("Event not found.");
            return;
        }
        SaleStatus[] statuses = SaleStatus.values();
        for (int index = 0; index < statuses.length; index++) {
            System.out.printf("%d. %s%n", index + 1, statuses[index]);
        }
        int selected = input.readIntMinMax("New status", 1, statuses.length,
                "Choose one of the listed statuses.");
        event.setStatus(statuses[selected - 1]);
        events.save(event);
        System.out.println("Updated event " + eventId + ".");
    }
}
