package model;

import model.enums.LockMechanism;

public class SimulationResult {
    private LockMechanism lockMechanism;
    private int threads;
    private int totalRequests;
    private int successCount;
    private int failedCount;
    private int negativeStockCount;
    private double elapsedMillis;
    private double tps;
    private double vsBaselinePercent;

    public SimulationResult(){};

    public SimulationResult(
        LockMechanism lockMechanism, int threads, int totalRequests, int successCount, int failedCount,
        int negativeStockCount, double elapsedMillis, double tps){
            this.lockMechanism = lockMechanism;
            this.threads = threads;
            this.totalRequests = totalRequests;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.negativeStockCount = negativeStockCount;
            this.elapsedMillis = elapsedMillis;
            this.tps = tps;
        }

        public LockMechanism getLockMechanism(){
            return lockMechanism;
        }

        public void setLockMechanism(LockMechanism lockMechanism){
            this.lockMechanism = lockMechanism;
        }

        public int getThreads(){
            return threads;
        }

        public void setThreads(int threads){
            this.threads = threads;
        }

        public int getTotalRequests(){
            return totalRequests;
        }

        public void setTotalRequests(int totalRequests){
            this.totalRequests = totalRequests;
        }

        public int getSuccessCount(){
            return successCount;
        }

        public void setSuccessCount(int successCount){
            this.successCount = successCount;
        }

        public int getFailedCount(){
            return failedCount;
        }

        public void setFailedCount(int failedCount){
            this.failedCount = failedCount;
        }

        public int getNegativeStockCount(){
            return negativeStockCount;
        }

        public void setNegativeStockCount(int negativeStockCount){
            this.negativeStockCount = negativeStockCount;
        }

        public double getElapsedMillis(){
            return elapsedMillis;
        }

        public void setElapsedMillis(double elapsedMillis){
            this.elapsedMillis = elapsedMillis;
        }

        public double getTps(){
            return tps;
        }

        public void setTps(double tps){
            this.tps = tps;
        }

        public double getVsBaselinePercent(){
            return vsBaselinePercent;
        }

        public void setVsBaselinePercent(double vsBaselinePercent){
            this.vsBaselinePercent = vsBaselinePercent;
        }

        public  String toAsciiTableRow() {
            return String.format("| %-15s | %-7d | %-7d | %-14d | %-10.2f | %-10.2f%% |",
                lockMechanism, threads, successCount, failedCount, negativeStockCount, elapsedMillis, tps, vsBaselinePercent
            );
        }
}


