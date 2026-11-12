package service;

import model.SimulationResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class SimulationReportService {
    private final Path reportDirectory;

    public SimulationReportService() {
        this(Path.of("data"));
    }

    public SimulationReportService(Path reportDirectory) {
        this.reportDirectory = Objects.requireNonNull(reportDirectory, "reportDirectory");
    }

    public ExportedReports export(List<SimulationResult> results) throws IOException {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("Simulation results must not be empty");
        }
        Path markdown = reportDirectory.resolve("simulation_report.md");
        Path csv = reportDirectory.resolve("simulation_results.csv");
        writeMarkdown(markdown, "Interactive simulation",
                "Results from the current simulation session.", results);
        writeCsv(csv, results);
        return new ExportedReports(markdown, csv);
    }

    public ExportedReports exportScenario(int stock, List<SimulationResult> results)
            throws IOException {
        if (stock < 0) {
            throw new IllegalArgumentException("stock must not be negative");
        }
        validateScenarioResults(stock, results);
        String fileStem = "simulation_stock_" + stock;
        Path markdown = reportDirectory.resolve(fileStem + ".md");
        Path csv = reportDirectory.resolve(fileStem + ".csv");
        writeMarkdown(markdown, "Initial stock = " + stock,
                "1,000 requests x 4 mechanisms x 3 runs; "
                        + "TPS = success / elapsed seconds.", results);
        writeCsv(csv, results);
        return new ExportedReports(markdown, csv);
    }

    public void writeMarkdown(
            Path destination,
            String scenario,
            String configuration,
            List<SimulationResult> results) throws IOException {
        try (AtomicCsvFile file = AtomicCsvFile.open(destination)) {
            BufferedWriter writer = file.writer();
            writer.write("# Flash Sale Simulator Experiment Report");
            writer.newLine();
            writer.newLine();
            writer.write("Scenario: " + scenario);
            writer.newLine();
            writer.write("Configuration: " + configuration);
            writer.newLine();
            writer.write("Exported at: " + LocalDateTime.now());
            writer.newLine();
            writer.newLine();
            writeSummaryTable(writer, results);
            writer.newLine();
            writer.write("## Raw Data by Run");
            writer.newLine();
            writer.newLine();
            writer.write("| Run | Mechanism | Threads | Accepted | Rejected | Initial Stock | Remaining Stock | Units Sold | Oversold Units | Retries | Duration (ms) | TPS | Consistent | Retry % | vs Baseline | Target |");
            writer.newLine();
            writer.write("|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---:|---:|---|");
            writer.newLine();
            for (SimulationResult result : results) {
                writer.write(String.format(Locale.ROOT,
                        "| %d | %s | %d | %d | %d | %d | %d | %d | %d | %d | %d | %.2f | %s | %.2f | %.1f%% | %s |%n",
                        result.getRunNumber(), result.getLockType(), result.getTotalThreads(),
                        result.getSuccessCount(), result.getFailedCount(), result.getInitialStock(),
                        result.getFinalStock(), result.getSuccessfulQuantity(), result.getNegativeStock(),
                        result.getRetryCount(), result.getDurationMs(), result.getTps(),
                        result.isDataConsistent() ? "Yes" : "No", retryRate(result),
                        result.getVsBaselinePercent(), targetStatus(result)));
            }
            file.commit();
        }
    }

    public void writeSummaryTable(BufferedWriter writer, List<SimulationResult> results)
            throws IOException {
        writer.write("## Summary by Mechanism");
        writer.newLine();
        writer.newLine();
        writer.write("| Mechanism | Runs | Avg TPS | Median TPS | Avg Success | Avg Negative Stock | Avg Retries | Avg vs Baseline | Consistent | Target |");
        writer.newLine();
        writer.write("|---|---:|---:|---:|---:|---:|---:|---:|---|---|");
        writer.newLine();
        for (Map.Entry<String, List<SimulationResult>> entry : groupByMechanism(results).entrySet()) {
            List<SimulationResult> runs = entry.getValue();
            double averageTps = runs.stream().mapToDouble(SimulationResult::getTps)
                    .average().orElse(0.0);
            double medianTps = medianTps(runs);
            double averageSuccess = runs.stream().mapToInt(SimulationResult::getSuccessCount)
                    .average().orElse(0.0);
            double averageNegative = runs.stream().mapToInt(SimulationResult::getNegativeStock)
                    .average().orElse(0.0);
            double averageRetries = runs.stream().mapToInt(SimulationResult::getRetryCount)
                    .average().orElse(0.0);
            double averageBaseline = runs.stream().mapToDouble(
                    SimulationResult::getVsBaselinePercent).average().orElse(0.0);
            boolean consistent = runs.stream().allMatch(SimulationResult::isDataConsistent);
            String target = entry.getKey().startsWith("NO_LOCK")
                    ? "BASELINE"
                    : consistent && averageNegative == 0.0 && averageBaseline >= -30.0
                            ? "DAT" : "CHUA_DAT";
            writer.write(String.format(Locale.ROOT,
                    "| %s | %d | %.2f | %.2f | %.1f | %.1f | %.1f | %.1f%% | %s | %s |%n",
                    entry.getKey(), runs.size(), averageTps, medianTps, averageSuccess,
                    averageNegative, averageRetries, averageBaseline,
                    consistent ? "CO" : "KHONG", target));
        }
    }

    public void writeCsv(Path destination, List<SimulationResult> results) throws IOException {
        try (AtomicCsvFile file = AtomicCsvFile.open(destination)) {
            BufferedWriter writer = file.writer();
            writer.write("runNumber,lockType,totalThreads,initialStock,successCount,successfulQuantity,failedCount,finalStock,negativeStock,durationMs,retryCount,retryRatePercent,tps,dataConsistent,vsBaselinePercent,target");
            writer.newLine();
            for (SimulationResult result : results) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.2f,%.2f,%b,%.1f,%s%n",
                        result.getRunNumber(), result.getLockType(), result.getTotalThreads(),
                        result.getInitialStock(), result.getSuccessCount(),
                        result.getSuccessfulQuantity(), result.getFailedCount(),
                        result.getFinalStock(), result.getNegativeStock(), result.getDurationMs(),
                        result.getRetryCount(), retryRate(result), result.getTps(),
                        result.isDataConsistent(), result.getVsBaselinePercent(), targetStatus(result)));
            }
            file.commit();
        }
    }

    public static double retryRate(SimulationResult result) {
        return result.getTotalThreads() == 0
                ? 0.0 : result.getRetryCount() * 100.0 / result.getTotalThreads();
    }

    public static String targetStatus(SimulationResult result) {
        if (result.getLockType().startsWith("NO_LOCK")) {
            return "BASELINE";
        }
        boolean safe = result.getNegativeStock() == 0 && result.isDataConsistent();
        return safe && result.getVsBaselinePercent() >= -30.0 ? "DAT" : "CHUA_DAT";
    }

    public static Map<String, List<SimulationResult>> groupByMechanism(
            List<SimulationResult> results) {
        Map<String, List<SimulationResult>> grouped = new LinkedHashMap<>();
        for (SimulationResult result : results) {
            grouped.computeIfAbsent(result.getLockType(), ignored -> new ArrayList<>())
                    .add(result);
        }
        return grouped;
    }

    public static double medianTps(List<SimulationResult> results) {
        double[] values = results.stream().mapToDouble(SimulationResult::getTps).sorted().toArray();
        int middle = values.length / 2;
        return values.length % 2 == 0
                ? (values[middle - 1] + values[middle]) / 2.0
                : values[middle];
    }

    public static void validateScenarioResults(int stock, List<SimulationResult> results) {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("Simulation results must not be empty");
        }
        for (SimulationResult result : results) {
            if (result == null || result.getInitialStock() != stock) {
                throw new IllegalArgumentException(
                        "Every simulation result must match the scenario stock");
            }
        }
    }

    public record ExportedReports(Path markdown, Path csv) {
    }
}
