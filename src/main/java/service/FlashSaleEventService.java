package service;

import exception.InvalidEventException;
import model.FlashSaleEvent;
import model.enums.SaleStatus;
import repository.FlashSaleEventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class FlashSaleEventService {
    private final FlashSaleEventRepository eventRepository;

    public FlashSaleEventService(FlashSaleEventRepository eventRepository) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository");
    }

    public FlashSaleEvent createEvent(FlashSaleEvent event) throws InvalidEventException {
        validateIdentity(event);
        FlashSalePolicy.validateEventSchedule(event);
        if (eventRepository.findById(event.getEventId()) != null) {
            throw new InvalidEventException("Event ID already exists: " + event.getEventId());
        }
        event.setStatus(SaleStatus.UPCOMING);
        event.setUnlockTime(null);
        eventRepository.save(event);
        return event;
    }

    public FlashSaleEvent startEvent(String eventId) throws InvalidEventException {
        return startEvent(eventId, LocalDateTime.now());
    }

    public FlashSaleEvent startEvent(String eventId, LocalDateTime now) throws InvalidEventException {
        FlashSaleEvent event = requireEvent(eventId);
        FlashSalePolicy.validateEventSchedule(event);
        LocalDateTime start = event.getStartTime();
        LocalDateTime end = event.getEndTime();
        if (now.isBefore(start)) {
            throw new InvalidEventException("The Flash Sale event has not started yet.");
        }
        if (!now.isBefore(end)) {
            throw new InvalidEventException("The Flash Sale event has ended.");
        }
        event.setStatus(SaleStatus.ACTIVE);
        eventRepository.save(event);
        return event;
    }

    public FlashSaleEvent endEvent(String eventId) throws InvalidEventException {
        FlashSaleEvent event = requireEvent(eventId);
        event.setStatus(SaleStatus.ENDED);
        eventRepository.save(event);
        return event;
    }

    public List<FlashSaleEvent> listEvents() {
        return eventRepository.findAll();
    }

    public FlashSaleEvent getEvent(String eventId) throws InvalidEventException {
        return requireEvent(eventId);
    }

    public FlashSaleEvent updateEvent(FlashSaleEvent update) throws InvalidEventException {
        validateIdentity(update);
        FlashSaleEvent current = requireEvent(update.getEventId());
        if (current.getStatus() == SaleStatus.ACTIVE
                || current.getStatus() == SaleStatus.LOCKED
                || current.getStatus().isEnded()) {
            throw new InvalidEventException(
                    "Only UPCOMING or INACTIVE events can be edited.");
        }
        FlashSalePolicy.validateEventSchedule(update);
        current.setName(update.getName());
        current.setStartTime(update.getStartTime());
        current.setEndTime(update.getEndTime());
        eventRepository.save(current);
        return current;
    }

    public boolean deleteEvent(String eventId) throws InvalidEventException {
        FlashSaleEvent event = requireEvent(eventId);
        if (event.getStatus() == SaleStatus.ACTIVE
                || event.getStatus() == SaleStatus.LOCKED) {
            throw new InvalidEventException("An active or locked event cannot be deleted.");
        }
        return eventRepository.deleteById(event.getEventId());
    }

    public FlashSaleEvent lockEvent(String eventId, LocalDateTime unlockAt)
            throws InvalidEventException {
        FlashSaleEvent event = requireEvent(eventId);
        if (event.getStatus() != SaleStatus.ACTIVE) {
            throw new InvalidEventException("Only ACTIVE events can be temporarily locked.");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = event.getEndTime();
        if (unlockAt == null || !unlockAt.isAfter(now) || unlockAt.isAfter(end)) {
            throw new InvalidEventException("Unlock time must be in the future and no later than the event end time.");
        }
        event.setStatus(SaleStatus.LOCKED);
        event.setUnlockTime(unlockAt);
        eventRepository.save(event);
        return event;
    }

    public FlashSaleEvent unlockEvent(String eventId) throws InvalidEventException {
        FlashSaleEvent event = requireEvent(eventId);
        if (event.getStatus() != SaleStatus.LOCKED) {
            throw new InvalidEventException("The event is not LOCKED.");
        }
        LocalDateTime end = event.getEndTime();
        if (!LocalDateTime.now().isBefore(end)) {
            event.setStatus(SaleStatus.ENDED);
            event.setUnlockTime(null);
            eventRepository.save(event);
            throw new InvalidEventException("The event has ended and cannot be reopened.");
        }
        event.setStatus(SaleStatus.ACTIVE);
        event.setUnlockTime(null);
        eventRepository.save(event);
        return event;
    }

    public FlashSaleEvent requireEvent(String eventId) throws InvalidEventException {
        if (eventId == null || eventId.isBlank()) {
            throw new InvalidEventException("Event ID is required.");
        }
        FlashSaleEvent event = eventRepository.findById(eventId.trim());
        if (event == null) {
            throw new InvalidEventException("Flash Sale event not found: " + eventId);
        }
        return event;
    }

    public static void validateIdentity(FlashSaleEvent event) throws InvalidEventException {
        if (event == null || event.getEventId() == null || event.getEventId().isBlank()) {
            throw new InvalidEventException("Event ID is required.");
        }
        if (event.getName() == null || event.getName().isBlank()) {
            throw new InvalidEventException("Event name is required.");
        }
    }

}
