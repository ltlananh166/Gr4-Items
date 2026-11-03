package controller;

import exception.InvalidDiscountException;
import exception.InvalidEventException;
import model.FlashSaleItem;
import model.FlashSaleEvent;
import model.enums.SaleStatus;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import service.FlashSaleEventService;
import service.FlashSalePolicy;

import java.time.LocalDateTime;
import java.util.List;

public class FlashSaleController {

    private final FlashSaleItemRepository flashItemRepository;
    private final FlashSaleEventService eventService;

    public FlashSaleController(FlashSaleItemRepository flashItemRepository) {
        this(flashItemRepository, (FlashSaleEventService) null);
    }

    public FlashSaleController(FlashSaleItemRepository flashItemRepository,
                               FlashSaleEventRepository eventRepository) {
        this(flashItemRepository, new FlashSaleEventService(eventRepository));
    }

    public FlashSaleController(FlashSaleItemRepository flashItemRepository,
                               FlashSaleEventService eventService) {
        this.flashItemRepository = flashItemRepository;
        this.eventService = eventService;
    }

    public List<FlashSaleItem> getFlashSaleItems(int limit) {
        return flashItemRepository.findAll().stream()
                .limit(Math.max(0, limit))
                .toList();
    }

    public FlashSaleItem addFlashSaleItem(FlashSaleItem item) throws InvalidDiscountException {
        if (item == null) {
            throw new IllegalArgumentException("Flash Sale item is required.");
        }
        FlashSalePolicy.validateDiscount(item);
        if (flashItemRepository.findById(item.getItemId()) != null) {
            throw new IllegalArgumentException("Flash Sale item ID already exists: " + item.getItemId());
        }
        if (!flashItemRepository.add(item)) {
            throw new IllegalArgumentException("Flash Sale item ID already exists: " + item.getItemId());
        }
        return item;
    }

    public FlashSaleItem findFlashSaleItem(String itemId) {
        return flashItemRepository.findById(itemId);
    }

    public List<FlashSaleItem> getItemsByEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return List.of();
        }
        return flashItemRepository.findAll().stream()
                .filter(item -> eventId.equalsIgnoreCase(item.getEventId()))
                .toList();
    }

    public boolean updateFlashSaleItem(FlashSaleItem item, int expectedVersion)
            throws InvalidDiscountException {
        FlashSalePolicy.validateDiscount(item);
        return flashItemRepository.updateItem(item, expectedVersion);
    }

    public boolean deleteFlashSaleItem(String itemId) {
        return flashItemRepository.deleteById(itemId);
    }

    public FlashSaleEvent createEvent(FlashSaleEvent event) throws InvalidEventException {
        return requireEventService().createEvent(event);
    }

    public FlashSaleEvent startEvent(String eventId) throws InvalidEventException {
        return requireEventService().startEvent(eventId);
    }

    public FlashSaleEvent startEvent(String eventId, LocalDateTime now) throws InvalidEventException {
        return requireEventService().startEvent(eventId, now);
    }

    public FlashSaleEvent endEvent(String eventId) throws InvalidEventException {
        return requireEventService().endEvent(eventId);
    }

    public List<FlashSaleEvent> listEvents() {
        return requireEventService().listEvents();
    }

    public List<FlashSaleEvent> listCustomerVisibleEvents() {
        return listEvents().stream()
                .filter(event -> event.getStatus() == SaleStatus.ACTIVE
                        || event.getStatus() == SaleStatus.LOCKED)
                .toList();
    }

    public FlashSaleEvent findEvent(String eventId) throws InvalidEventException {
        return requireEventService().getEvent(eventId);
    }

    public FlashSaleEvent updateEvent(FlashSaleEvent event) throws InvalidEventException {
        return requireEventService().updateEvent(event);
    }

    public boolean deleteEvent(String eventId) throws InvalidEventException {
        if (!getItemsByEvent(eventId).isEmpty()) {
            throw new InvalidEventException(
                    "Delete all Flash Sale items before deleting the event.");
        }
        return requireEventService().deleteEvent(eventId);
    }

    public FlashSaleEvent lockEvent(String eventId, LocalDateTime unlockAt)
            throws InvalidEventException {
        return requireEventService().lockEvent(eventId, unlockAt);
    }

    public FlashSaleEvent unlockEvent(String eventId) throws InvalidEventException {
        return requireEventService().unlockEvent(eventId);
    }

    private FlashSaleEventService requireEventService() {
        if (eventService == null) {
            throw new IllegalStateException("FlashSaleEventService is not configured.");
        }
        return eventService;
    }
}


// Thành viên 3
