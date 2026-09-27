package model;

import model.enums.LockMechanism;
import java.time.LocalDateTime;

public class SimulationRecord {
    private String flashItemId;
    private int threadId;
    private LockMechanism lockMechanism;
    private boolean isSuccess;
    private String failReason;
    private LocalDateTime processedAt;
    private long processingTimeMs;

    public SimulationRecord() {
        this.processedAt = LocalDateTime.now();
    }

    public SimulationRecord(
        String flashItemId, int threadId, LockMechanism lockMechanism, boolean isSuccess, String failReason,
        long processingTimeMs) {
        this.flashItemId = flashItemId;
        this.threadId = threadId;
        this.lockMechanism = lockMechanism;
        this.isSuccess = isSuccess;
        this.failReason = failReason;
        this.processedAt = LocalDateTime.now();
        this.processingTimeMs = processingTimeMs;
    }

    public String getFlashItemId() {
        return flashItemId;
    }

    public void setFlashItemId(String flashItemId) {
        this.flashItemId = flashItemId;
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public LockMechanism getLockMechanism() {
        return lockMechanism;
    }

    public void setLockMechanism(LockMechanism lockMechanism) {
        this.lockMechanism = lockMechanism;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public  String toCsvLine() {
        return  String.format("%s,%d,%s,%b,%s,%s,%d", flashItemId, threadId, lockMechanism, isSuccess, failReason == null ? "NONE" : failReason,
        processedAt, processingTimeMs);
    }
}


