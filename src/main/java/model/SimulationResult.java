package model;

import model.enums.LockMechanism;

public class SimulationResult {
    private String lockType;
    private int runNumber;
    private int totalThreads;
    private int initialStock;
    private int successCount;
    private int successfulQuantity;
    private int failedCount;
    private int finalStock;
    private int negativeStock;
    private long durationMs;
    private int retryCount;
    private double tps;
    private boolean dataConsistent;
    private double vsBaselinePercent;

    public SimulationResult() {
    }

    public String getLockType() {
        return lockType;
    }

    public void setLockType(String lockType) {
        this.lockType = lockType;
    }

    public int getRunNumber() {
        return runNumber;
    }

    public void setRunNumber(int runNumber) {
        this.runNumber = runNumber;
    }

    public int getTotalThreads() {
        return totalThreads;
    }

    public void setTotalThreads(int totalThreads) {
        this.totalThreads = totalThreads;
    }

    public int getInitialStock() {
        return initialStock;
    }

    public void setInitialStock(int initialStock) {
        this.initialStock = initialStock;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getSuccessfulQuantity() {
        return successfulQuantity;
    }

    public void setSuccessfulQuantity(int successfulQuantity) {
        this.successfulQuantity = successfulQuantity;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getFinalStock() {
        return finalStock;
    }

    public void setFinalStock(int finalStock) {
        this.finalStock = finalStock;
    }

    public int getNegativeStock() {
        return negativeStock;
    }

    public void setNegativeStock(int negativeStock) {
        this.negativeStock = negativeStock;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public double getTps() {
        return tps;
    }

    public void setTps(double tps) {
        this.tps = tps;
    }

    public boolean isDataConsistent() {
        return dataConsistent;
    }

    public void setDataConsistent(boolean dataConsistent) {
        this.dataConsistent = dataConsistent;
    }

    public double getVsBaselinePercent() {
        return vsBaselinePercent;
    }

    public void setVsBaselinePercent(double vsBaselinePercent) {
        this.vsBaselinePercent = vsBaselinePercent;
    }

    public LockMechanism getLockMechanism() {
        if (lockType == null) {
            return null;
        }
        return switch (lockType.replace("_AVG", "")) {
            case "NO_LOCK" -> LockMechanism.NO_LOCK;
            case "SYNCHRONIZED" -> LockMechanism.SYNCHRONIZED;
            case "FILE_LOCK" -> LockMechanism.FILE_LOCK;
            case "OPTIMISTIC_LOCK", "OPTIMISTIC" -> LockMechanism.OPTIMISTIC;
            default -> null;
        };
    }

    public int getThreads() {
        return totalThreads;
    }

    public int getTotalRequests() {
        return totalThreads;
    }

    public int getOversoldUnits() {
        return negativeStock;
    }

    public int getQtyPerThread() {
        return totalThreads == 0 ? 0 : successfulQuantity / totalThreads;
    }

    public double getElapsedMillis() {
        return durationMs;
    }

    public String toAsciiTableRow() {
        return String.format(
                "| %-15s | %-7d | %-7d | %-7d | %-7d | %-14d | %-10.2f | %-10.2f | %12.2f%% |",
                lockType, totalThreads, getQtyPerThread(), successCount, failedCount,
                negativeStock, (double) durationMs, tps, vsBaselinePercent);
    }
}
