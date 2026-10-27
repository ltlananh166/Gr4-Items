package model;

import java.time.LocalDateTime;
import model.enums.SaleStatus;

public class FlashSaleEvent extends BaseEntity {
    private String eventId;
    private String name;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime unlockTime;
    private SaleStatus status;

    public FlashSaleEvent() {
        this.status = SaleStatus.UPCOMING;
    }

    public FlashSaleEvent(String eventId, String title, String description,
            LocalDateTime startTime, LocalDateTime endTime, SaleStatus status) {
        setEventId(eventId);
        this.name = title;
        this.description = description;
        setStartTime(startTime);
        setEndTime(endTime);
        this.status = status == null ? SaleStatus.UPCOMING : status;
        validateTimeRange();
    }

    @Override
    public String getId() { return eventId; }

    @Override
    public String toCsvLine() {
        return String.join(",", value(eventId), value(name), value(startTime),
                value(endTime), status.name(), value(unlockTime));
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == SaleStatus.ACTIVE
                && (startTime == null || !now.isBefore(startTime))
                && (endTime == null || now.isBefore(endTime));
    }

    private void validateTimeRange() {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Event end time must be after start time.");
        }
    }

    private static String value(Object value) { return value == null ? "" : value.toString(); }

    public String getEventId() { return eventId; }
    @Override
    public void setId(String id) { this.eventId = id; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTitle() { return name; }
    public void setTitle(String title) { this.name = title; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; validateTimeRange(); }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; validateTimeRange(); }
    public LocalDateTime getUnlockTime() { return unlockTime; }
    public void setUnlockTime(LocalDateTime unlockTime) { this.unlockTime = unlockTime; }
    public SaleStatus getStatus() { return status; }
    public void setStatus(SaleStatus status) { this.status = status == null ? SaleStatus.UPCOMING : status; }
}
