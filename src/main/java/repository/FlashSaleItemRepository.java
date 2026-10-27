package repository;

import model.FlashSaleItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlashSaleItemRepository extends CsvRepository<FlashSaleItem> {
    private static final int MAX_RETRY = 5;
    private final String fileLockPath = "flashsale_item.lock";
    private final Map<String, FlashSaleItem> inMemorydb = new ConcurrentHashMap<>();

    public FlashSaleItemRepository() {
        this("data");
    }

    public FlashSaleItemRepository(String dataDirectory) {
        super(FlashSaleItem.class, dataDirectory, "flash_items.csv",
                "itemId,productId,eventId,productName,originalPrice,salePrice,"
                        + "initialStock,soldQty,remainingStock,version");
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

    /** Kept for compatibility with the misspelled method used by older code. */
    @Deprecated
    public FlashSaleItem finndById(String itemId) {
        return findById(itemId);
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

    // No Lock
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
            // Intentionally writes the previously-read value without locking to
            // demonstrate the lost-update race in NO_LOCK mode.
            item.recordSaleFromObservedStock(currentStock, qty);
            return true;
        }
        return false;
    }

    // Synchronized lock
    public synchronized boolean sellWithSynchronized(String itemId, int qty) {
        FlashSaleItem  item = inMemorydb.get(itemId);
        if (item == null) return false;

        if (item.getStock() >= qty) {
            item.recordSale(qty);
            return true;
        }
        return false;
    }

    // File lock
    public synchronized boolean sellWithFileLock(String itemId, int qty) {
        File file = new File(fileLockPath);
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel channel = raf.getChannel();
            FileLock lock = channel.lock())
        {
            FlashSaleItem item = inMemorydb.get(itemId);
            if (item != null && item.getStock() >= qty) {
                item.recordSale(qty);
                return true;
            } 
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // Optimistic lock
    public boolean sellWithOptimisticLock(String itemId, int qty, int maxRetries) {
        int retries = maxRetries > 0 ? maxRetries : MAX_RETRY;
        for (int attempt = 0; attempt < retries; attempt++) {
            FlashSaleItem item = inMemorydb.get(itemId);
            if (item == null || item.getStock() < qty) {
                return false;
            }

            int currentVersion = item.getVersion();
            int currentStock = item.getStock();

            synchronized (item) {
                if (item.getVersion() == currentVersion) {
                    item.recordSale(qty);
                    return true;
                }
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
}


