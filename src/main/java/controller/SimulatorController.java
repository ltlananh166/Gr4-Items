package controller;

import model.FlashSaleItem;
import model.SimulationRecord;
import model.SimulationResult;
import model.enums.LockMechanism;
import repository.FlashSaleItemRepository;
import repository.SimulationRecordRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulatorController implements AutoCloseable {
    private static final int MAX_RETRY = 5;

    private final FlashSaleItemRepository itemRepo;
    private final SimulationRecordRepository recordRepo;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public SimulatorController() {
        this(new FlashSaleItemRepository(), new SimulationRecordRepository());
    }

    public SimulatorController(FlashSaleItemRepository itemRepo,
                               SimulationRecordRepository recordRepo) {
        this.itemRepo = Objects.requireNonNull(itemRepo, "itemRepo");
        this.recordRepo = Objects.requireNonNull(recordRepo, "recordRepo");
    }

    public void resetStock(String flashItemId, int resetQty) {
        if (resetQty < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }
        itemRepo.resetStock(flashItemId, resetQty);
        itemRepo.persist();
        recordRepo.deleteAll();
    }

    public List<FlashSaleItem> getFlashItems() {
        return itemRepo.findAll();
    }

    public void resetToInitialStock(String flashItemId) {
        itemRepo.resetToInitialStock(flashItemId);
        itemRepo.persist();
        recordRepo.deleteAll();
    }

    public SimulationResult runSimulation(String flashItemId, int threads,
                                           int qtyPerThread, LockMechanism lockType) {
        if (flashItemId == null || flashItemId.isBlank()) {
            throw new IllegalArgumentException("Flash item ID is required.");
        }
        if (threads < 1 || qtyPerThread < 1) {
            throw new IllegalArgumentException("Threads and quantity per request must be positive.");
        }
        Objects.requireNonNull(lockType, "lockType");

        FlashSaleItem initialItem = itemRepo.findById(flashItemId);
        if (initialItem == null) {
            throw new IllegalArgumentException("Flash sale item not found: " + flashItemId);
        }
        int initialStock = initialItem.getStock();

        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCounter = new AtomicInteger();
        AtomicInteger failedCounter = new AtomicInteger();

        for (int threadId = 0; threadId < threads; threadId++) {
            int currentThreadId = threadId;
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    long requestStart = System.nanoTime();
                    boolean success = switch (lockType) {
                        case NO_LOCK -> itemRepo.sellWithNoLock(flashItemId, qtyPerThread);
                        case SYNCHRONIZED -> itemRepo.sellWithSynchronized(flashItemId, qtyPerThread);
                        case FILE_LOCK -> itemRepo.sellWithFileLock(flashItemId, qtyPerThread);
                        case OPTIMISTIC -> itemRepo.sellWithOptimisticLock(
                                flashItemId, qtyPerThread, MAX_RETRY);
                    };
                    long durationMs = Math.max(0,
                            (System.nanoTime() - requestStart) / 1_000_000L);

                    if (success) {
                        successCounter.incrementAndGet();
                    } else {
                        failedCounter.incrementAndGet();
                    }
                    recordRepo.save(new SimulationRecord(flashItemId, currentThreadId,
                            lockType, success, success ? null : "OUT_OF_STOCK_OR_CONFLICT",
                            durationMs));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    failedCounter.incrementAndGet();
                } catch (RuntimeException failure) {
                    failedCounter.incrementAndGet();
                    recordRepo.save(new SimulationRecord(flashItemId, currentThreadId,
                            lockType, false, failure.getMessage(), 0));
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        long simulationStart = System.nanoTime();
        try {
            readyLatch.await();
            startLatch.countDown();
            doneLatch.await();
        } catch (InterruptedException interrupted) {
            startLatch.countDown();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Simulation was interrupted.", interrupted);
        }

        double elapsedMs = Math.max(1.0,
                (System.nanoTime() - simulationStart) / 1_000_000.0);
        itemRepo.persist();
        int successCount = successCounter.get();
        int failedCount = failedCounter.get();
        int soldUnits = successCount * qtyPerThread;
        int oversoldUnits = Math.max(0, soldUnits - initialStock);
        double tps = successCount / (elapsedMs / 1_000.0);

        return new SimulationResult(lockType, threads, threads, successCount,
                failedCount, oversoldUnits, elapsedMs, tps);
    }

    public List<SimulationResult> compareAllMechanisms(String flashItemId, int threads) {
        List<SimulationResult> results = new ArrayList<>();
        FlashSaleItem item = itemRepo.findById(flashItemId);
        if (item == null) {
            throw new IllegalArgumentException("Flash sale item not found: " + flashItemId);
        }
        int initialStock = item.getInitialStock();

        for (LockMechanism mechanism : LockMechanism.values()) {
            resetStock(flashItemId, initialStock);
            results.add(runSimulation(flashItemId, threads, 1, mechanism));
        }

        if (!results.isEmpty()) {
            double baselineTps = results.get(0).getTps();
            for (SimulationResult result : results) {
                double difference = baselineTps == 0
                        ? 0
                        : ((result.getTps() - baselineTps) / baselineTps) * 100.0;
                result.setVsBaselinePercent(difference);
            }
        }
        return results;
    }

    /** Backwards-compatible alias for the original misspelled method. */
    @Deprecated
    public List<SimulationResult> compareAllMachanism(String flashItemId, int threads) {
        return compareAllMechanisms(flashItemId, threads);
    }

    public void shutdownExecutor() {
        executorService.shutdown();
    }

    @Deprecated
    public void shutdownExcutor() {
        shutdownExecutor();
    }

    @Override
    public void close() {
        shutdownExecutor();
    }
}
