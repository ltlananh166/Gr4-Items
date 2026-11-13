package view;

import controller.SimulationReportController;
import controller.SimulatorController;
import model.SimulationResult;
import model.enums.CustTier;
import model.enums.LockType;
import service.SimulationReportService;
import service.SimulatorPerformanceTarget;
import service.SimulatorService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResearcherView {
    private final SimulatorController simulatorController;
    private final SimulationReportController reportController;
    private final ConsoleInput input;
    private final Map<CustTier, Double> tierComposition = new EnumMap<>(CustTier.class);

    private int threads = 500;
    private int stock = 100;
    private List<SimulationResult> lastResults = new ArrayList<>();

    public ResearcherView(SimulatorController simulatorController,
                          SimulationReportController reportController,
                          ConsoleInput input) {
        this.simulatorController = simulatorController;
        this.reportController = reportController;
        this.input = input;
        this.tierComposition.put(CustTier.STANDARD, 0.60);
        this.tierComposition.put(CustTier.SILVER, 0.20);
        this.tierComposition.put(CustTier.GOLD, 0.15);
        this.tierComposition.put(CustTier.DIAMOND, 0.05);
    }

    public void display() {
        boolean back = false;
        while (!back) {
            this.printMenu();
            try {
                switch (this.input.readInt("Select an option", 0)) {
                    case 1 -> this.configureQuickSimulation();
                    case 2 -> this.runSingle();
                    case 3 -> this.runQuickBenchmark();
                    case 4 -> this.runRequiredBenchmark();
                    case 5 -> this.exportReport();
                    case 0 -> back = true;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (exception.OperationCancelledException exception) {
                System.out.println("Simulation cancelled.");
            } catch (IllegalArgumentException exception) {
                System.out.println("Invalid configuration: " + exception.getMessage());
            }
        }
    }

    public void printMenu() {
        System.out.println("\n===== RESEARCHER MENU =====");
        System.out.println("1. Configure quick test (100-500 customer threads)");
        System.out.println("2. Run one locking mechanism");
        System.out.println("3. Quick benchmark (4 mechanisms, 1 run each)");
        System.out.println("4. Required benchmark (1,000 threads x 4 mechanisms x 3 runs; averages)");
        System.out.println("5. Export CSV and Markdown results");
        System.out.println("0. Back");
        System.out.println("--- Simulation scenario ---");
        System.out.println("Test inventory only; real product stock is not changed.");
        System.out.println("Flash Sale item: FI-SIM-001 | Synthetic product: P-SIM-001");
        System.out.printf("Initial stock: %d units (maximum 1,000) | Customer tier mix: %s%n",
                this.stock, this.formatTierComposition());
        System.out.printf("Quick test: %d customer threads | Max optimistic retries: %d%n",
                this.threads, SimulatorService.MAX_RETRY);
        System.out.println("---------------------------");
    }

    private String formatTierComposition() {
        return String.format(Locale.ROOT, "STANDARD %.0f%%, SILVER %.0f%%, GOLD %.0f%%, DIAMOND %.0f%%",
                this.tierComposition.getOrDefault(CustTier.STANDARD, 0.0) * 100,
                this.tierComposition.getOrDefault(CustTier.SILVER, 0.0) * 100,
                this.tierComposition.getOrDefault(CustTier.GOLD, 0.0) * 100,
                this.tierComposition.getOrDefault(CustTier.DIAMOND, 0.0) * 100);
    }

    public void configureQuickSimulation() {
        this.threads = this.input.readIntMinMax("Number of customer threads", SimulatorService.QUICK_MIN_THREADS,
                SimulatorService.QUICK_MAX_THREADS, "Thread count must be between 100 and 500.");
        this.stock = this.input.readIntMinMax("Initial Flash Sale stock (units)", 0, 1000,
                "Stock must be between 0 and 1,000 units.");
        int standard = this.input.readIntMin("STANDARD share (%)", 0, "Share cannot be negative.");
        int silver = this.input.readIntMin("SILVER share (%)", 0, "Share cannot be negative.");
        int gold = this.input.readIntMin("GOLD share (%)", 0, "Share cannot be negative.");
        int diamond = this.input.readIntMin("DIAMOND share (%)", 0, "Share cannot be negative.");
        int total = standard + silver + gold + diamond;
        if (total <= 0) {
            throw new IllegalArgumentException("Total tier share must be greater than zero.");
        }
        this.tierComposition.put(CustTier.STANDARD, standard / (double) total);
        this.tierComposition.put(CustTier.SILVER, silver / (double) total);
        this.tierComposition.put(CustTier.GOLD, gold / (double) total);
        this.tierComposition.put(CustTier.DIAMOND, diamond / (double) total);
    }

    public void runSingle() {
        LockType lockType = this.input.readLockType("Locking mechanism", LockType.OPTIMISTIC_LOCK);
        this.lastResults = List.of(this.simulatorController.runSingle(lockType, this.threads, this.stock,
                SimulatorService.MAX_RETRY, this.tierComposition));
        this.displayResults(this.lastResults);
    }

    public void runQuickBenchmark() {
        this.lastResults = this.simulatorController.runQuickBenchmark(
                this.threads, this.stock, SimulatorService.MAX_RETRY, this.tierComposition);
        this.displayResults(this.lastResults);
    }

    public void runRequiredBenchmark() {
        System.out.println("Running required benchmark (1,000 threads x 4 mechanisms x 3 runs)...");
        this.lastResults = this.simulatorController.runBenchmark(
                SimulatorService.FULL_THREADS, this.stock, SimulatorService.FULL_REPEATS,
                SimulatorService.MAX_RETRY, this.tierComposition);
        this.displayResults(this.lastResults);
    }

    public void displayResults(List<SimulationResult> results) {
        if (results == null || results.isEmpty()) {
            System.out.println("No simulation results available.");
            return;
        }
        System.out.println();
        System.out.println("--- Purchase behavior ---");
        System.out.println("Each thread represents one customer purchase request.");
        System.out.println("STANDARD customers buy 1 unit; SILVER, GOLD, and DIAMOND customers buy 1 or 2 units at random.");
        System.out.println("Consistent = Yes when recorded sold quantity and remaining stock match successful units sold, with no overselling.");
        System.out.println("TPS = accepted purchase requests per second.");
        System.out.println("-------------------------");
        boolean hasBaseline = results.stream().anyMatch(result -> result.getLockType() != null
                && result.getLockType().startsWith(LockType.NO_LOCK.name()));
        if (results.stream().anyMatch(result -> result.getLockType().endsWith("_AVG"))) {
            System.out.println("Average values across 3 runs.");
        }
        System.out.println("\nSimulation results (stock is for Flash Sale item FI-SIM-001):");
        System.out.printf("%-18s  %8s  %10s  %10s  %8s  %9s  %10s  %8s  %12s  %12s  %15s  %12s  %12s%n",
                "Mechanism", "Threads", "Accepted", "Rejected", "Initial",
                "Remaining", "Units sold", "Retries", "Duration(ms)",
                "TPS", "vs Baseline %", "Target", "Consistent");
        for (SimulationResult result : results) {
            String baselineText = hasBaseline
                    ? String.format(Locale.ROOT, "%.1f%%", result.getVsBaselinePercent())
                    : "N/A";
            String targetText = hasBaseline
                    ? this.targetLabel(SimulatorPerformanceTarget.evaluate(result))
                    : "N/A";
            System.out.printf(Locale.ROOT, "%-18s  %8d  %10d  %10d  %8d  %9d  %10d  %8d  %12d  %12.2f  %15s  %12s  %12s%n",
                    this.readableMechanism(result.getLockType()), result.getTotalThreads(),
                    result.getSuccessCount(), result.getFailedCount(), result.getInitialStock(),
                    result.getFinalStock(), result.getSuccessfulQuantity(),
                    result.getRetryCount(), result.getDurationMs(), result.getTps(), baselineText,
                    targetText, result.isDataConsistent() ? "Yes" : "No");
        }
        System.out.println("\n");
    }

    private String targetLabel(SimulatorPerformanceTarget.Verdict verdict) {
        return switch (verdict) {
            case BASELINE -> "BASELINE";
            case DAT -> "MET";
            case CHUA_DAT -> "NOT MET";
        };
    }

    private String readableMechanism(String mechanism) {
        return mechanism.replace("_AVG", "")
                .replace("NO_LOCK", "No Lock")
                .replace("FILE_LOCK", "File Lock")
                .replace("SYNCHRONIZED", "Synchronized")
                .replace("OPTIMISTIC_LOCK", "Optimistic Lock");
    }

    public void exportReport() {
        if (this.lastResults.isEmpty()) {
            System.out.println("There are no results to export.");
            return;
        }
        try {
            SimulationReportService.ExportedReports reports = this.reportController.export(this.lastResults);
            System.out.println("Markdown exported: " + reports.markdown());
            System.out.println("CSV exported: " + reports.csv());
        } catch (IOException exception) {
            System.out.println("Unable to export report: " + exception.getMessage());
        }
    }
}
