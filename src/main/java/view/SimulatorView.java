package view;

import controller.SimulatorController;
import model.SimulationResult;
import model.enums.LockType;

import java.util.List;
import java.util.Objects;

public class SimulatorView {
    private static final int MAX_THREADS = 1000;

    private final ConsoleInput input;
    private final SimulatorController controller;

    public SimulatorView(ConsoleInput input, SimulatorController controller) {
        this.input = Objects.requireNonNull(input, "input");
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    public void start() {
        boolean back = false;
        while (!back) {
            System.out.println("\n===== FLASH SALE SIMULATOR =====");
            System.out.println("1. Run one locking mechanism");
            System.out.println("2. Run quick benchmark");
            System.out.println("0. Back");
            int choice = input.readIntMinMax("Select an option: ", 0, 2,
                    "Select 0, 1, or 2.");
            switch (choice) {
                case 1 -> runSingle();
                case 2 -> runQuickBenchmark();
                case 0 -> back = true;
                default -> throw new IllegalStateException("Invalid simulation choice.");
            }
        }
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

    private void runSingle() {
        LockType lockType = input.readLockType("Locking mechanism", LockType.OPTIMISTIC_LOCK);
        int threads = input.readIntMinMax("Number of threads", 1, MAX_THREADS,
                "Thread count must be between 1 and 1,000.");
        int stock = input.readIntMin("Initial stock", 0, "Stock cannot be negative.");
        displayResults(List.of(controller.runSingle(lockType, threads, stock)));
    }

    private void runQuickBenchmark() {
        int threads = input.readIntMinMax("Number of threads", 100, 500,
                "Quick benchmark requires 100 to 500 threads.");
        int stock = input.readIntMin("Initial stock", 0, "Stock cannot be negative.");
        displayResults(controller.runQuickBenchmark(threads, stock));
    }
}
