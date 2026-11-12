package controller;

import model.SimulationResult;
import model.enums.CustTier;
import model.enums.LockType;
import service.SimulatorService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SimulatorController {
    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = Objects.requireNonNull(simulatorService, "simulatorService");
    }

    public SimulationResult runSingle(LockType lockType, int threads, int stock) {
        return this.simulatorService.runSimulation(lockType, threads, stock);
    }

    public SimulationResult runSingle(
            LockType lockType,
            int threads,
            int stock,
            int maxRetries,
            Map<CustTier, Double> tierComposition) {
        return this.simulatorService.runSimulation(
                lockType, threads, stock, maxRetries, tierComposition);
    }

    public List<SimulationResult> runAll(int threads, int stock) {
        return this.applyBaseline(this.runAllInternal(threads, stock, null, null));
    }

    public List<SimulationResult> runAll(
            int threads,
            int stock,
            int maxRetries,
            Map<CustTier, Double> tierComposition) {
        return this.applyBaseline(this.runAllInternal(
                threads, stock, maxRetries, tierComposition));
    }

    public List<SimulationResult> runQuickBenchmark(int threads, int stock) {
        return this.applyBaseline(this.simulatorService.runQuickBenchmark(threads, stock));
    }

    public List<SimulationResult> runQuickBenchmark(
            int threads,
            int stock,
            int maxRetries,
            Map<CustTier, Double> tierComposition) {
        return this.applyBaseline(this.simulatorService.runQuickBenchmark(
                threads, stock, maxRetries, tierComposition));
    }

    public List<SimulationResult> runFullBenchmark(int stock) {
        return this.applyBaselinePerRun(this.simulatorService.runFullBenchmark(stock));
    }

    public List<SimulationResult> runBenchmark(int threads, int stock, int repeats) {
        return this.runBenchmarkInternal(threads, stock, repeats, null, null);
    }

    public List<SimulationResult> runBenchmark(
            int threads,
            int stock,
            int repeats,
            int maxRetries,
            Map<CustTier, Double> tierComposition) {
        return this.runBenchmarkInternal(
                threads, stock, repeats, maxRetries, tierComposition);
    }

    public List<LockType> getMechanisms() {
        return Arrays.asList(LockType.values());
    }

    public List<SimulationResult> runAllInternal(
            int threads,
            int stock,
            Integer maxRetries,
            Map<CustTier, Double> tierComposition) {
        List<SimulationResult> results = new ArrayList<>(LockType.values().length);
        for (LockType lockType : LockType.values()) {
            SimulationResult result = maxRetries == null
                    ? this.simulatorService.runSimulation(lockType, threads, stock)
                    : this.simulatorService.runSimulation(
                            lockType, threads, stock, maxRetries, tierComposition);
            results.add(result);
        }
        return results;
    }

    public List<SimulationResult> runBenchmarkInternal(
            int threads,
            int stock,
            int repeats,
            Integer maxRetries,
            Map<CustTier, Double> tierComposition) {
        if (repeats <= 0) {
            throw new IllegalArgumentException("repeats must be positive");
        }
        Map<LockType, List<SimulationResult>> grouped = new LinkedHashMap<>();
        for (LockType lockType : LockType.values()) {
            grouped.put(lockType, new ArrayList<>());
        }
        for (int runNumber = 1; runNumber <= repeats; runNumber++) {
            for (SimulationResult result : this.runAllInternal(
                    threads, stock, maxRetries, tierComposition)) {
                LockType lockType = LockType.valueOf(result.getLockType());
                result.setRunNumber(runNumber);
                grouped.get(lockType).add(result);
            }
        }

        List<SimulationResult> averages = new ArrayList<>(LockType.values().length);
        for (Map.Entry<LockType, List<SimulationResult>> entry : grouped.entrySet()) {
            averages.add(this.average(entry.getKey(), entry.getValue(), threads, stock));
        }
        return this.applyBaseline(averages);
    }

    public List<SimulationResult> applyBaselinePerRun(List<SimulationResult> results) {
        for (int runNumber = 1; runNumber <= SimulatorService.FULL_REPEATS; runNumber++) {
            List<SimulationResult> currentRun = new ArrayList<>();
            for (SimulationResult result : results) {
                if (result.getRunNumber() == runNumber) {
                    currentRun.add(result);
                }
            }
            this.applyBaseline(currentRun);
        }
        return results;
    }

    public List<SimulationResult> applyBaseline(List<SimulationResult> results) {
        double baselineTps = results.stream()
                .filter(result -> result.getLockType().startsWith(LockType.NO_LOCK.name()))
                .mapToDouble(SimulationResult::getTps)
                .findFirst()
                .orElse(0.0);
        for (SimulationResult result : results) {
            if (baselineTps > 0.0) {
                double percent = ((result.getTps() - baselineTps) / baselineTps) * 100.0;
                result.setVsBaselinePercent(Math.round(percent * 10.0) / 10.0);
            }
        }
        return results;
    }

    public SimulationResult average(
            LockType lockType,
            List<SimulationResult> results,
            int threads,
            int stock) {
        int count = results.size();
        SimulationResult average = new SimulationResult();
        average.setLockType(lockType.name() + "_AVG");
        average.setTotalThreads(threads);
        average.setInitialStock(stock);
        average.setSuccessCount(this.roundAverage(
                results.stream().mapToInt(SimulationResult::getSuccessCount).sum(), count));
        average.setSuccessfulQuantity(this.roundAverage(
                results.stream().mapToInt(SimulationResult::getSuccessfulQuantity).sum(), count));
        average.setFailedCount(this.roundAverage(
                results.stream().mapToInt(SimulationResult::getFailedCount).sum(), count));
        average.setFinalStock(this.roundAverage(
                results.stream().mapToInt(SimulationResult::getFinalStock).sum(), count));
        average.setNegativeStock(this.roundAverage(
                results.stream().mapToInt(SimulationResult::getNegativeStock).sum(), count));
        average.setDurationMs(Math.round(
                results.stream().mapToLong(SimulationResult::getDurationMs).average().orElse(0.0)));
        average.setRetryCount(this.roundAverage(
                results.stream().mapToInt(SimulationResult::getRetryCount).sum(), count));
        average.setTps(results.stream().mapToDouble(SimulationResult::getTps).average().orElse(0.0));
        average.setDataConsistent(results.stream().allMatch(SimulationResult::isDataConsistent));
        return average;
    }

    public int roundAverage(int total, int count) {
        return Math.round((float) total / count);
    }
}

