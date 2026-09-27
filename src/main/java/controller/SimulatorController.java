package controller;

import model.FlashSaleItem;
import model.SimulationRecord;
import model.SimulationResult;
import model.enums.LockMechanism;
import repository.FlashSaleItemRepository;
import repository.SimulationRecordRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulatorController {
    private FlashSaleItemRepository itemRepo;
    private SimulationRecordRepository recordRepo;
    private ExecutorService executorService;
    private int MAX_RETRY = 5;
    private int THREAD_POOL_SIZE = 50;

    public SimulatorController() {
        this.itemRepo = new FlashSaleItemRepository();
        this.recordRepo = new SimulationRecordRepository();
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public SimulatorController(FlashSaleItemRepository itemRepo, SimulationRecordRepository recordRepo) {
        this.itemRepo = itemRepo;
        this.recordRepo = recordRepo;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void resetStock(String flashItemId, int resetQty) {
        itemRepo.resetStock(flashItemId, resetQty);
        recordRepo.deleteAll();
    }

    public SimulationResult runSimulation(String flahItemId, int threads, int qtyPerThread, LockMechanism locktype) {
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch starLatch = new CountDownLatch(1);
        CountDownLatch doneLath = new CountDownLatch(threads);

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failedCounter = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            int threadId = i;
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    starLatch.await();
                    long start = System.currentTimeMillis();
                    boolean success = false;

                    switch (locktype) {
                        case NO_LOCK:
                            success = itemRepo.sellWithNoLock(flahItemId, qtyPerThread); 
                            break;
                        case SYNCHRONIZED:
                            success = itemRepo.sellWithSynchronized(flahItemId, qtyPerThread);
                            break;
                        case FILE_LOCK:
                            success = itemRepo.sellWithFileLock(flahItemId, qtyPerThread);
                            break;
                        case OPTIMISTIC:
                            success = itemRepo.sellWithOptimisticLock(flahItemId, qtyPerThread, MAX_RETRY);
                            break;
                    }

                    long duration = System.currentTimeMillis() - start;
                    if (success) {
                        successCounter.incrementAndGet();
                    } else {
                        failedCounter.incrementAndGet();
                    }

                    recordRepo.save(new SimulationRecord(flahItemId, threadId, locktype, success, success ? null : "OUT_OF_STOCK_OR_CONFLICT", duration));
                } catch (InterruptedException ignored) {
                } finally {
                    doneLath.countDown();
                }
            });
        }   
        try {
            readyLatch.await();
            long testStart = System.currentTimeMillis();
            starLatch.countDown();
            doneLath.await();
            long testEnd = System.currentTimeMillis();

            double elapsedMs = Math.max(1, testEnd - testStart);
            int success = successCounter.get();
            int failed = failedCounter.get();
            double tps = calculateTps(success, elapsedMs);

            FlashSaleItem finalItem = itemRepo.finndById(flahItemId);
            int remainingStock = finalItem != null ? finalItem.getStock() : 0;
            int negativeStock = remainingStock < 0 ? Math.abs(remainingStock) : 0;

            return new SimulationResult(locktype, threads, threads, success, failed, negativeStock, elapsedMs, tps);

        } catch (InterruptedException e) {
            return  null;
        }
    }

    public List<SimulationResult> compareAllMachanism(String flashItemId, int threads) {
        List<SimulationResult> results = new ArrayList<>();
        int initialStock = 100;

        for (LockMechanism mechanism : LockMechanism.values()) {
            resetStock(flashItemId, initialStock);
            SimulationResult result = runSimulation(flashItemId, threads, 1, mechanism);
            if (result != null) {
                results.add(result);
            }
        }

        if (!results.isEmpty()) {
            double baselineTps = results.get(0).getTps();
            for (SimulationResult r : results) {
                double diff = ((r.getTps() - baselineTps) / (baselineTps == 0 ? 1 : baselineTps)) * 100;
                r.setVsBaselinePercent(diff);
            }
        }
        return results;
    }   

    public void shutdownExcutor() {
        executorService.shutdown();
    }

    private double calculateTps(int successCount, double elapsedMs) {
        return (successCount / (elapsedMs / 1000.0));
    }

}

   

 

