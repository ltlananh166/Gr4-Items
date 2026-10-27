
package repository;

import model.FlashSaleEvent;
import model.enums.SaleStatus;

import java.util.List;
import java.util.stream.Collectors;

public class FlashSaleEventRepository
        extends CsvRepository<FlashSaleEvent> {

    private static final String FILE_NAME = "flash_events.csv";

    private static final String HEADER =
            "eventId,title,description,startTime,endTime,status";

    public FlashSaleEventRepository() {
        super(FlashSaleEvent.class, FILE_NAME, HEADER);
    }

    public FlashSaleEventRepository(String dataDirectory) {
        super(FlashSaleEvent.class, dataDirectory, FILE_NAME, HEADER);
    }

    public String generateNewEventId() {
        int maxId = findAll().stream()
                .map(FlashSaleEvent::getEventId)
                .filter(id -> id != null && id.matches("E-\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(2)))
                .max()
                .orElse(0);

        return String.format("E-%05d", maxId + 1);
    }

    public List<FlashSaleEvent> findByStatus(SaleStatus status) {
        return findAll().stream()
                .filter(event -> status != null
                        && status == event.getStatus())
                .collect(Collectors.toList());
    }

    public List<FlashSaleEvent> findActiveEvents() {
        return findAll().stream()
                .filter(FlashSaleEvent::isActive)
                .collect(Collectors.toList());
    }
}
