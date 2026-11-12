package model;

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
        return this.lockType;
    }

    public void setLockType(String lockType) {
        this.lockType = lockType;
    }

    public int getRunNumber() {
        return this.runNumber;
    }

    public void setRunNumber(int runNumber) {
        this.runNumber = runNumber;
    }

    public int getTotalThreads() {
        return this.totalThreads;
    }

    public void setTotalThreads(int totalThreads) {
        this.totalThreads = totalThreads;
    }

    public int getInitialStock() {
        return this.initialStock;
    }

    public void setInitialStock(int initialStock) {
        this.initialStock = initialStock;
    }

    public int getSuccessCount() {
        return this.successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getSuccessfulQuantity() {
        return this.successfulQuantity;
    }

    public void setSuccessfulQuantity(int successfulQuantity) {
        this.successfulQuantity = successfulQuantity;
    }

    public int getFailedCount() {
        return this.failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getFinalStock() {
        return this.finalStock;
    }

    public void setFinalStock(int finalStock) {
        this.finalStock = finalStock;
    }

    public int getNegativeStock() {
        return this.negativeStock;
    }

    public void setNegativeStock(int negativeStock) {
        this.negativeStock = negativeStock;
    }

    public long getDurationMs() {
        return this.durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public int getRetryCount() {
        return this.retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public double getTps() {
        return this.tps;
    }

    public void setTps(double tps) {
        this.tps = tps;
    }

    public boolean isDataConsistent() {
        return this.dataConsistent;
    }

    public void setDataConsistent(boolean dataConsistent) {
        this.dataConsistent = dataConsistent;
    }

    public double getVsBaselinePercent() {
        return this.vsBaselinePercent;
    }

    public void setVsBaselinePercent(double vsBaselinePercent) {
        this.vsBaselinePercent = vsBaselinePercent;
    }

    public int getThreads() {
        return this.totalThreads;
    }

    public int getTotalRequests() {
        return this.totalThreads;
    }

    public int getOversoldUnits() {
        return this.negativeStock;
    }

    public int getQtyPerThread() {
        return this.totalThreads == 0 ? 0 : this.successfulQuantity / this.totalThreads;
    }

    public double getElapsedMillis() {
        return this.durationMs;
    }

    public String toAsciiTableRow() {
        return String.format(
                "| %-15s | %-7d | %-7d | %-7d | %-7d | %-14d | %-10.2f | %-10.2f | %12.2f%% |",
                this.lockType, this.totalThreads, this.getQtyPerThread(), this.successCount,
                this.failedCount, this.negativeStock, (double) this.durationMs,
                this.tps, this.vsBaselinePercent);
    }
}
