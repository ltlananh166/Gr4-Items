package view;

import controller.SimulationReportController;
import controller.SimulatorController;
import model.SimulationResult;
import model.enums.CustTier;
import model.enums.LockType;
import service.SimulationReportService;
import service.SimulatorService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
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
        tierComposition.put(CustTier.STANDARD, 0.60);
        tierComposition.put(CustTier.SILVER, 0.20);
        tierComposition.put(CustTier.GOLD, 0.15);
        tierComposition.put(CustTier.DIAMOND, 0.05);
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Select an option", 0)) {
                    case 1 -> configureQuickSimulation();
                    case 2 -> runSingle();
                    case 3 -> runQuickBenchmark();
                    case 4 -> runRequiredBenchmark();
                    case 5 -> exportReport();
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
        System.out.printf("Quick test: %d threads, stock %d, maximum optimistic retries %d%n",
                threads, stock, SimulatorService.MAX_RETRY);
        System.out.println("1. Configure quick test (100–500 threads)");
        System.out.println("2. Run one mechanism");
        System.out.println("3. Quick benchmark: 4 mechanisms");
        System.out.println("4. Required benchmark: 1,000 × 4 × 3, with averages");
        System.out.println("5. Export CSV + Markdown results");
        System.out.println("0. Back");
    }

    public void configureQuickSimulation() {
        threads = input.readIntMinMax("Number of threads", SimulatorService.QUICK_MIN_THREADS,
                SimulatorService.QUICK_MAX_THREADS, "Thread count must be between 100 and 500.");
        stock = input.readIntMin("Initial stock", 0, "Stock cannot be negative.");
        int standard = input.readIntMin("STANDARD share (%)", 0, "Share cannot be negative.");
        int silver = input.readIntMin("SILVER share (%)", 0, "Share cannot be negative.");
        int gold = input.readIntMin("GOLD share (%)", 0, "Share cannot be negative.");
        int diamond = input.readIntMin("DIAMOND share (%)", 0, "Share cannot be negative.");
        int total = standard + silver + gold + diamond;
        if (total <= 0) {
            throw new IllegalArgumentException("Total tier share must be greater than zero.");
        }
        tierComposition.put(CustTier.STANDARD, standard / (double) total);
        tierComposition.put(CustTier.SILVER, silver / (double) total);
        tierComposition.put(CustTier.GOLD, gold / (double) total);
        tierComposition.put(CustTier.DIAMOND, diamond / (double) total);
    }

    public void runSingle() {
        LockType lockType = input.readLockType("Locking mechanism", LockType.OPTIMISTIC_LOCK);
        lastResults = List.of(simulatorController.runSingle(lockType, threads, stock,
                SimulatorService.MAX_RETRY, tierComposition));
        displayResults(lastResults);
    }

    public void runQuickBenchmark() {
        lastResults = simulatorController.runQuickBenchmark(threads, stock);
        displayResults(lastResults);
    }

    public void runRequiredBenchmark() {
        System.out.println("Running 1,000 threads × 4 mechanisms × 3 runs. Please wait...");
        lastResults = simulatorController.runBenchmark(
                SimulatorService.FULL_THREADS, stock, SimulatorService.FULL_REPEATS,
                SimulatorService.MAX_RETRY, tierComposition);
        displayResults(lastResults);
    }

    public void displayResults(List<SimulationResult> results) {
        if (results == null || results.isEmpty()) {
            System.out.println("No simulation results available.");
            return;
        }
        System.out.printf("%-20s %8s %8s %8s %12s %12s %10s %12s%n",
                "Mechanism", "Threads", "Success", "Failures",
                "Final stock", "Retry", "TPS", "Consistent");
        for (SimulationResult result : results) {
            System.out.printf("%-20s %8d %8d %8d %12d %12d %10.2f %12s%n",
                    result.getLockType(), result.getTotalThreads(), result.getSuccessCount(),
                    result.getFailedCount(), result.getFinalStock(), result.getRetryCount(),
                    result.getTps(), result.isDataConsistent() ? "Yes" : "No");
        }
    }

    public void exportReport() {
        if (lastResults.isEmpty()) {
            System.out.println("There are no results to export.");
            return;
        }
        try {
            SimulationReportService.ExportedReports reports = reportController.export(lastResults);
            System.out.println("Markdown exported: " + reports.markdown());
            System.out.println("CSV exported: " + reports.csv());
        } catch (IOException exception) {
            System.out.println("Unable to export report: " + exception.getMessage());
        }
    }
}


