package repository;

import model.FlashSaleItem;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlashSaleItemRepository {
    private int MAX_RETRY = 5;
    private String fileLockPath = "flashsale_item.lock";
    private Map<String, FlashSaleItem> inMemorydb = new ConcurrentHashMap<>();
    
    public FlashSaleItemRepository() {
        // MOCK CODE
        inMemorydb.put("ITEM001", new FlashSaleItem("ITEM001", "EVT001", "iPhone 15 Pro", 100, 999.0, 1));
    }

    public FlashSaleItem finndById(String itemId) {
        return inMemorydb.get(itemId);
    }

    public void resetStock(String itemId, int resetQty) {
        FlashSaleItem item = inMemorydb.get(itemId);
        if (item != null) {
            item.setStock(resetQty);
            item.setVersion(1);
        }
    }

    // No Lock
    public boolean sellWithNoLock(String itemId, int qty) {
        FlashSaleItem item = inMemorydb.get(itemId);
        if (item == null) return false;

        int currentStock = item.getStock();
        if (currentStock >= qty) {
            try {
                Thread.sleep(1);
            }
            catch (InterruptedException ignored) {}
            item.setStock(currentStock - qty);
            return true;
        }
        return false;
    } 

    // Synchronized lock
    public synchronized boolean sellWithSynchronized(String itemId, int qty) {
        FlashSaleItem  item = inMemorydb.get(itemId);
        if (item == null) return false;

        if (item.getStock() >= qty) {
            item.setStock(item.getStock() - qty);
            return true;
        }
        return false;
    }

    // File lock
    public boolean sellWithFileLock(String itemId, int qty) {
        File file = new File(fileLockPath);
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel channel = raf.getChannel();
            FileLock lock = channel.tryLock()) 
        {
            FlashSaleItem item = inMemorydb.get(itemId);
            if (item != null && item.getStock() >= qty) {
                item.setStock(item.getStock() - qty);
                return true;
            } 
            return false;
        } 
        catch (Exception e) {
            return false;
        }
    }

    // Optimistic lock
    public boolean sellWithOptimisticLock(String itemId, int qty, int maxRetries) {
        int retries = (maxRetries > 0) ? maxRetries : this.MAX_RETRY;
        for (int attempt = 0; attempt < retries; attempt++) {
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
                    return true;
                }
            }

            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {}
        }
        return false;
    }
}





