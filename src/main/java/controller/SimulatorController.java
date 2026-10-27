package controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import model.FlashSaleItem;
import model.SimulationRecord;
import model.SimulationResult;
import model.enums.LockMechanism;
import repository.FlashSaleItemRepository;
import repository.SimulationRecordRepository;

public class SimulatorController {
    private static final int MAX_RETRY = 5;

    private final FlashSaleItemRepository itemRepo;
    private final SimulationRecordRepository recordRepo;

    public SimulatorController() {
        this(new FlashSaleItemRepository(), new SimulationRecordRepository());
    }

    public SimulatorController(FlashSaleItemRepository itemRepo,
            SimulationRecordRepository recordRepo) {
        this.itemRepo = itemRepo;
        this.recordRepo = recordRepo;
    }

    public void resetStock(String flashItemId, int resetQty) {
        itemRepo.resetStock(flashItemId, resetQty);
        recordRepo.deleteAll();
    }

    public SimulationResult runSimulation(String flashItemId, int threads, int qtyPerThread,
            LockMechanism lockMechanism) {
        if (threads < 1 || qtyPerThread < 1 || lockMechanism == null) {
            throw new IllegalArgumentException("Threads, quantity, and lock mechanism are required.");
        }
        if (threads > 2000) {
            throw new IllegalArgumentException("Thread count cannot exceed 2000.");
        }
        if (itemRepo.finndById(flashItemId) == null) {
            throw new IllegalArgumentException("Flash Sale item does not exist: " + flashItemId);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCounter = new AtomicInteger();
        AtomicInteger failedCounter = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threads; i++) {
                final int threadId = i;
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    long start = 0L;
                    try {
                        startLatch.await();
                        start = System.nanoTime();
                        boolean success = sell(flashItemId, qtyPerThread, lockMechanism);
                        long duration = (System.nanoTime() - start) / 1_000_000L;
                        if (success) {
                            successCounter.incrementAndGet();
                        } else {
                            failedCounter.incrementAndGet();
                        }
                        recordRepo.save(new SimulationRecord(flashItemId, threadId, lockMechanism,
                                success, success ? null : "OUT_OF_STOCK_OR_CONFLICT", duration));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Simulation worker was interrupted.", e);
                    } finally {
                        doneLatch.countDown();
                    }
                }));
            }
            readyLatch.await();
            long testStart = System.nanoTime();
            startLatch.countDown();
            doneLatch.await();
            double elapsedMillis = Math.max(1.0, (System.nanoTime() - testStart) / 1_000_000.0);
            for (Future<?> future : futures) {
                future.get();
            }

            int success = successCounter.get();
            int failed = failedCounter.get();
            return new SimulationResult(lockMechanism, threads, threads, success, failed,
                    itemRepo.getOversoldQuantity(), elapsedMillis, calculateTps(success, elapsedMillis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Simulation was interrupted.", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("A simulation worker failed.", cause);
        } finally {
            startLatch.countDown();
            executor.shutdownNow();
        }
    }

    public List<SimulationResult> compareAllMechanisms(String flashItemId, int threads) {
        List<SimulationResult> results = new ArrayList<>();
        recordRepo.deleteAll();
        for (LockMechanism mechanism : LockMechanism.values()) {
            itemRepo.resetStock(flashItemId, 100);
            results.add(runSimulation(flashItemId, threads, 1, mechanism));
        }
        if (!results.isEmpty()) {
            double baselineTps = results.get(0).getTps();
            for (SimulationResult result : results) {
                double diff = baselineTps == 0 ? 0
                        : ((result.getTps() - baselineTps) / baselineTps) * 100;
                result.setVsBaselinePercent(diff);
            }
        }
        return results;
    }

    public List<SimulationRecord> getRecords() {
        return recordRepo.getAll();
    }

    public void exportResults(Path outputFile) throws IOException {
        Path parent = outputFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        List<String> lines = new ArrayList<>();
        lines.add("flashItemId,threadId,lockMechanism,success,failReason,processedAt,processingTimeMs");
        for (SimulationRecord record : recordRepo.getAll()) {
            lines.add(record.toCsvLine());
        }
        Files.write(outputFile, lines, StandardCharsets.UTF_8);
    }

    private boolean sell(String itemId, int quantity, LockMechanism mechanism) {
        switch (mechanism) {
            case NO_LOCK:
                return itemRepo.sellWithNoLock(itemId, quantity);
            case SYNCHRONIZED:
                return itemRepo.sellWithSynchronized(itemId, quantity);
            case REENTRANT_LOCK:
                return itemRepo.sellWithReentrantLock(itemId, quantity);
            case FILE_LOCK:
                return itemRepo.sellWithFileLock(itemId, quantity);
            case OPTIMISTIC:
                return itemRepo.sellWithOptimisticLock(itemId, quantity, MAX_RETRY);
            default:
                throw new IllegalArgumentException("Unsupported lock mechanism: " + mechanism);
        }
    }

    private double calculateTps(int successCount, double elapsedMs) {
        return successCount / (elapsedMs / 1000.0);
    }
}
