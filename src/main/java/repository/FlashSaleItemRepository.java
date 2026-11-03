package repository;

import model.FlashSaleItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FlashSaleItemRepository extends CsvRepository<FlashSaleItem> {
    public static final int MAX_RETRY = 5;
    private final Path fileLockPath;
    private final Map<String, FlashSaleItem> inMemorydb = new ConcurrentHashMap<>();

    public FlashSaleItemRepository() {
        this("data");
    }

    public FlashSaleItemRepository(String dataDirectory) {
        super(FlashSaleItem.class, dataDirectory, "flash_items.csv",
                "itemId,productId,eventId,productName,originalPrice,salePrice,"
                        + "initialStock,soldQty,remainingStock,version");
        this.fileLockPath = getFilePath().resolveSibling("flash_items.csv.lck");
        for (FlashSaleItem item : super.findAll()) {
            inMemorydb.put(item.getId(), item);
        }
    }

    @Override
    public java.util.List<FlashSaleItem> findAll() {
        java.util.List<FlashSaleItem> items = new ArrayList<>(inMemorydb.values());
        items.sort(Comparator.comparing(FlashSaleItem::getId));
        return items;
    }

    @Override
    public FlashSaleItem findById(String itemId) {
        return itemId == null ? null : inMemorydb.get(itemId);
    }

    @Override
    public synchronized void save(FlashSaleItem item) {
        super.save(item);
        inMemorydb.put(item.getId(), item);
    }

    /** Thêm mặt hàng mới vào CSV và bộ nhớ đệm mô phỏng trong bộ nhớ. */
    public synchronized boolean add(FlashSaleItem item) {
        if (item == null || item.getId() == null || item.getId().isBlank()) {
            throw new IllegalArgumentException("Flash sale item and ID are required.");
        }
        if (inMemorydb.containsKey(item.getId())) {
            return false;
        }
        super.save(item);
        inMemorydb.put(item.getId(), item);
        return true;
    }

    public synchronized void resetStock(String itemId, int resetQty) {
        FlashSaleItem item = inMemorydb.get(itemId);
        if (item != null) {
            item.setInitialStock(resetQty);
            item.setStock(resetQty);
            item.setSoldQuantity(0);
            item.setVersion(1);
        }
    }

    public synchronized void resetToInitialStock(String itemId) {
        FlashSaleItem item = inMemorydb.get(itemId);
        if (item != null) {
            item.resetToInitialStock();
        }
    }

    public synchronized void persist() {
        super.saveAll(new ArrayList<>(inMemorydb.values()));
    }

    public synchronized boolean updateItem(FlashSaleItem item, int expectedVersion) {
        if (item == null || item.getId() == null) {
            return false;
        }
        FlashSaleItem current = inMemorydb.get(item.getId());
        if (current == null || current.getVersion() != expectedVersion
                || !super.update(item, expectedVersion)) {
            return false;
        }
        inMemorydb.put(item.getId(), item);
        return true;
    }

    public synchronized boolean deleteById(String itemId) {
        if (itemId == null || inMemorydb.remove(itemId) == null) {
            return false;
        }
        super.saveAll(new ArrayList<>(inMemorydb.values()));
        return true;
    }

    // Không khóa
    public boolean sellWithNoLock(String itemId, int qty) {
        FlashSaleItem item = inMemorydb.get(itemId);
        if (item == null) return false;

        int currentStock = item.getStock();
        if (currentStock >= qty) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }

            item.recordSaleFromObservedStock(currentStock, qty);
            return true;
        }
        return false;
    }

    // Khóa đồng bộ
    public synchronized boolean sellWithSynchronized(String itemId, int qty) {
        FlashSaleItem  item = inMemorydb.get(itemId);
        if (item == null) return false;

        if (item.getStock() >= qty) {
            item.recordSale(qty);
            return true;
        }
        return false;
    }

    // Khóa tệp
    public synchronized boolean sellWithFileLock(String itemId, int qty) {
        try (RandomAccessFile raf = new RandomAccessFile(fileLockPath.toFile(), "rw");
            FileChannel channel = raf.getChannel();
            FileLock lock = channel.lock())
        {
            FlashSaleItem item = inMemorydb.get(itemId);
            if (item != null && item.getStock() >= qty) {
                item.recordSale(qty);
                return true;
            } 
            return false;
        } catch (IOException | OverlappingFileLockException exception) {
            throw new IllegalStateException("Cannot acquire Flash Sale item file lock", exception);
        }
    }

    // Khóa lạc quan
    public boolean sellWithOptimisticLock(String itemId, int qty, int maxRetries) {
        return sellWithOptimisticLock(itemId, qty, maxRetries, null);
    }

    public boolean sellWithOptimisticLock(
            String itemId, int qty, int maxRetries, AtomicInteger retryCount) {
        int retries = maxRetries > 0 ? maxRetries : MAX_RETRY;
        for (int attempt = 0; attempt < retries; attempt++) {
            FlashSaleItem item = inMemorydb.get(itemId);
            if (item == null || item.getStock() < qty) {
                return false;
            }

            int currentVersion = item.getVersion();

            synchronized (item) {
                if (item.getVersion() == currentVersion) {
                    item.recordSale(qty);
                    return true;
                }
            }

            if (retryCount != null) {
                retryCount.incrementAndGet();
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public boolean sellWithOptimisticLock(String itemId, int qty) {
        return sellWithOptimisticLock(itemId, qty, MAX_RETRY);
    }
}
