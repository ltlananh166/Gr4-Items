package repository;

import model.FlashSaleItem;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class FlashSaleItemRepository {
    private final String fileLockPath = "flashsale_item.lock";
    private final Map<String, FlashSaleItem> inMemorydb = new ConcurrentHashMap<>();
    private final AtomicInteger successfulQuantity = new AtomicInteger();
    private final ReentrantLock reentrantLock = new ReentrantLock();
    private volatile int initialStock = 100;

    public FlashSaleItemRepository() {
        inMemorydb.put("ITEM001", new FlashSaleItem("ITEM001", "EVT001", "iPhone 15 Pro", 100, 999.0, 1));
    }

    public FlashSaleItem finndById(String itemId) {
        return inMemorydb.get(itemId);
    }

    public void resetStock(String itemId, int resetQty) {
        if (resetQty < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }
        FlashSaleItem item = Objects.requireNonNull(inMemorydb.get(itemId),
                "Flash Sale item does not exist: " + itemId);
        synchronized (item) {
            item.setStock(resetQty);
            item.setSoldQuantity(0);
            item.setVersion(1);
            successfulQuantity.set(0);
            initialStock = resetQty;
        }
    }

    public boolean sellWithNoLock(String itemId, int qty) {
        validateQuantity(qty);
        FlashSaleItem item = inMemorydb.get(itemId);
        if (item == null) return false;

        int currentStock = item.getStock();
        if (currentStock >= qty) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            item.setStock(currentStock - qty);
            successfulQuantity.addAndGet(qty);
            item.setVersion(item.getVersion() + 1);
            return true;
        }
        return false;
    }

    public synchronized boolean sellWithSynchronized(String itemId, int qty) {
        FlashSaleItem item = inMemorydb.get(itemId);
        return sell(item, qty);
    }

    public boolean sellWithFileLock(String itemId, int qty) {
        validateQuantity(qty);
        FlashSaleItem item = inMemorydb.get(itemId);
        if (item == null) return false;
        synchronized (item) {
            File file = new File(fileLockPath);
            try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    FileChannel channel = raf.getChannel();
                    FileLock lock = channel.lock()) {
                if (!lock.isValid()) {
                    throw new IOException("Could not acquire the Flash Sale file lock.");
                }
                return sell(item, qty);
            } catch (IOException e) {
                throw new IllegalStateException("Could not lock Flash Sale stock.", e);
            }
        }
    }

    public boolean sellWithReentrantLock(String itemId, int qty) {
        validateQuantity(qty);
        reentrantLock.lock();
        try {
            return sell(inMemorydb.get(itemId), qty);
        } finally {
            reentrantLock.unlock();
        }
    }

    public boolean sellWithOptimisticLock(String itemId, int qty, int maxRetries) {
        validateQuantity(qty);
        if (maxRetries < 1) {
            throw new IllegalArgumentException("Retry count must be positive.");
        }
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            FlashSaleItem item = inMemorydb.get(itemId);
            if (item == null || item.getStock() < qty) {
                return false;
            }

            int currentVersion = item.getVersion();
            int currentStock = item.getStock();

            synchronized (item) {
                if (item.getVersion() == currentVersion) {
                    item.setStock(currentStock - qty);
                    item.setVersion(currentVersion + 1);
                    successfulQuantity.addAndGet(qty);
                    return true;
                }
            }

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public int getOversoldQuantity() {
        return Math.max(0, successfulQuantity.get() - initialStock);
    }

    private boolean sell(FlashSaleItem item, int qty) {
        validateQuantity(qty);
        if (item == null || item.getStock() < qty) {
            return false;
        }
        item.setStock(item.getStock() - qty);
        item.setVersion(item.getVersion() + 1);
        successfulQuantity.addAndGet(qty);
        return true;
    }

    private static void validateQuantity(int qty) {
        if (qty < 1) {
            throw new IllegalArgumentException("Sale quantity must be positive.");
        }
    }
}




