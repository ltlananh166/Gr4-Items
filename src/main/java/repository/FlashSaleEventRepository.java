
package repository;

import config.FlashSaleFormats;
import model.FlashSaleEvent;
import model.enums.SaleStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class FlashSaleEventRepository extends CsvRepository<FlashSaleEvent> {

    private final ThreadLocal<Boolean> isSyncing =
            ThreadLocal.withInitial(() -> false);

    public FlashSaleEventRepository() {
        super(
                FlashSaleEvent.class,
                "flash_events.csv",
                "eventId,name,startTime,endTime,status,unlockTime"
        );
        syncEventStatuses();
    }

    public FlashSaleEventRepository(String dataDirectory) {
        super(
                FlashSaleEvent.class,
                dataDirectory,
                "flash_events.csv",
                "eventId,name,startTime,endTime,status,unlockTime"
        );
        syncEventStatuses();
    }

    @Override
    public List<FlashSaleEvent> findAll() {
        if (!isSyncing.get()) {
            syncEventStatuses();
        }

        return super.findAll();
    }

    public void syncEventStatuses() {
        if (isSyncing.get()) {
            return;
        }

        isSyncing.set(true);

        try {
            LocalDateTime now = LocalDateTime.now();
            List<FlashSaleEvent> allEvents = super.findAll();
            boolean changed = false;

            for (FlashSaleEvent event : allEvents) {

                // Unlock an event when its unlock time has arrived.
                if (event.getSaleStatus() == SaleStatus.LOCKED) {
                    String unlockTime = event.getUnlockTime();

                    if (unlockTime != null
                            && !unlockTime.trim().isEmpty()) {

                        LocalDateTime unlock = parseTime(
                                unlockTime, event.getEventId());

                        if (!now.isBefore(unlock)) {
                            event.setStatus(SaleStatus.ACTIVE);
                            event.setUnlockTime("");
                            changed = true;
                        }
                    }
                }

                // End an active event when its end time has arrived.
                if (event.getSaleStatus() == SaleStatus.ACTIVE) {
                    LocalDateTime end = parseTime(
                            event.getEndTime(), event.getEventId());

                    if (!now.isBefore(end)) {
                        event.setStatus(SaleStatus.ENDED);
                        changed = true;
                    }
                }
            }

            if (changed) {
                super.saveAll(allEvents);
            }

        } finally {
            isSyncing.set(false);
        }
    }

    private static LocalDateTime parseTime(
            String value, String eventId) {

        try {
            return LocalDateTime.parse(
                    value, FlashSaleFormats.EVENT_TIME);

        } catch (DateTimeParseException
                | NullPointerException e) {

            throw new IllegalStateException(
                    "Thời gian không hợp lệ của sự kiện " + eventId,
                    e
            );
        }
    }
}
