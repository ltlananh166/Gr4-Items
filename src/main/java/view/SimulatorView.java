package view;

import controller.SimulatorController;
import model.SimulationResult;
import model.enums.LockMechanism;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class SimulatorView {
    private final ConsoleInput input;
    private final SimulatorController controller;

    public SimulatorView(ConsoleInput input, SimulatorController controller) {
        this.input = Objects.requireNonNull(input, "input");
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    public void start() {
        boolean back = false;
        while (!back) {
            printMenu();
            int choice = input.readIntMinMax("Choose an option", 0, 2,
                    "Choose 0, 1, or 2.");
            switch (choice) {
                case 1 -> handleSingleTest();
                case 2 -> handleCompareAll();
                case 0 -> back = true;
                default -> throw new IllegalStateException("Validated simulator choice was out of range.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n========================================");
        System.out.println("   FLASH SALE MULTI-THREAD SIMULATOR");
        System.out.println("1. Test one lock mechanism");
        System.out.println("2. Benchmark all four lock mechanisms");
        System.out.println("0. Back to main menu");
        System.out.println("========================================");
    }

    private void handleSingleTest() {
        String flashItemId = selectFlashItem();
        int threads = readThreadCount();
        System.out.println("\nLock mechanisms:");
        LockMechanism[] mechanisms = LockMechanism.values();
        for (int i = 0; i < mechanisms.length; i++) {
            System.out.printf("%d. %s%n", i + 1, mechanisms[i]);
        }
        int lockChoice = input.readIntMinMax("Choose a lock mechanism", 1,
                mechanisms.length, "Choose one of the listed mechanisms.");

        controller.resetToInitialStock(flashItemId);
        System.out.printf("\nRunning %d simultaneous requests...%n", threads);
        SimulationResult result = controller.runSimulation(flashItemId, threads,
                1, mechanisms[lockChoice - 1]);
        printSingleResult(result);
    }

    private void handleCompareAll() {
        String flashItemId = selectFlashItem();
        int threads = readThreadCount();
        System.out.println("\nRunning benchmark for all four lock mechanisms...");
        List<SimulationResult> results = controller.compareAllMechanisms(flashItemId, threads);

        System.out.println("\n================ PERFORMANCE COMPARISON ================");
        System.out.printf("| %-15s | %-7s | %-7s | %-7s | %-14s | %-10s | %-10s | %-12s |%n",
                "Lock", "Threads", "Success", "Failed", "Oversold units", "Time (ms)",
                "TPS", "vs baseline");
        System.out.println("--------------------------------------------------------------------------");
        for (SimulationResult result : results) {
            System.out.println(result.toAsciiTableRow());
        }
        System.out.println("==========================================================");
    }

    private String selectFlashItem() {
        List<model.FlashSaleItem> items = controller.getFlashItems();
        if (items.isEmpty()) {
            throw new IllegalStateException("No flash sale items found in data/flash_items.csv.");
        }

        System.out.println("\nAvailable Flash Sale items (first 20):");
        new FlashSaleView().displayItems(items.subList(0, Math.min(20, items.size())));
        String defaultId = items.get(0).getId();
        while (true) {
            String itemId = input.readLine("Flash sale item ID (Enter for "
                    + defaultId + "): ");
            if (itemId.isEmpty()) {
                return defaultId;
            }
            if (items.stream().anyMatch(item -> itemId.equals(item.getId()))) {
                return itemId;
            }
            System.out.println("Unknown item ID. Choose an ID from the list.");
        }
    }

    private int readThreadCount() {
        return input.readIntMinMax("Number of concurrent requests", 1, 1000,
                "Enter a number from 1 to 1000.");
    }

    private void printSingleResult(SimulationResult result) {
        System.out.println("\n--- SIMULATION RESULT ---");
        System.out.println("Lock mechanism: " + result.getLockMechanism());
        System.out.println("Threads: " + result.getThreads());
        System.out.println("Success count: " + result.getSuccessCount());
        System.out.println("Failed count: " + result.getFailedCount());
        System.out.println("Oversold units: " + result.getOversoldUnits()
                + (result.getOversoldUnits() > 0
                ? " -> [OVERSELLING DETECTED]"
                : " -> [SAFE - NO OVERSELLING]"));
        System.out.printf("Throughput (TPS): %.2f requests/s%n", result.getTps());
    }

    /** Standalone entry point retained for the original simulator branch. */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ConsoleInput input = new ConsoleInput(scanner);
        SimulatorController controller = new SimulatorController();
        try {
            new SimulatorView(input, controller).start();
        } finally {
            controller.shutdownExecutor();
        }
    }
}
