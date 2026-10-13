package view;

import controller.SimulatorController;
import model.SimulationResult;
import model.enums.LockMechanism;

import java.util.List;
import java.util.Scanner;

public class SimulatorView {
    private SimulatorController controller = new SimulatorController();
    private Scanner scanner = new Scanner(System.in);

    public void start() {
        while (true) {
            
            System.out.println("\n==================================");
            System.out.println("   FLASH SALE MULTI-THREAD SIMULATOR SYSTEM");
            System.out.println("1. Test a specific Lock Mechanism");
            System.out.println("2. Run a Benchmark to compare all 4 lock mechanisms (Measure TPS & Overselling)");
            System.out.println("0. Exit");
            System.out.print(">> Choose a option: ");

            int choice = scanner.nextInt();
            if (choice == 0) {
                controller.shutdownExecutor();
                System.out.println(">> Simulator shut down.");
                break;
            }

            switch (choice) {
                case 1:
                    handleSingleTest();
                    break;
                case 2:
                    handleCompareAll();
                    break;
                default:
                    System.out.println(">> Invalid option! Try again.");
            }
        }
    }

    private void handleSingleTest() {
        System.out.print(">> Enter number of threads: ");
        int threads = scanner.nextInt();

        System.out.println("Choose a lock mechanism:");
        System.out.println("1. NO_LOCK");
        System.out.println("2. SYNCHRONIZE");
        System.out.println("3. FILE_LOCK");
        System.out.println("4. OPTIMISTIC");
        System.out.print(">> Choose: ");
        int lockChoice = scanner.nextInt();

        LockMechanism mechanism = LockMechanism.values()[lockChoice - 1];

        controller.resetStock("ITEM001", 10);
        System.out.println("\n>> Processing with " + threads + " threads...");

        SimulationResult result = controller.runSimulation("ITEM001", threads, 1, mechanism);
        printSingleResult(result);
    }

    private void handleCompareAll() {
        System.out.print(">> Enter number of threads: ");
        int threads = scanner.nextInt();

        System.out.println("\n>> Executing Auto-Benchmark on 4 lock mechanisms...");
        List<SimulationResult> results = controller.compareAllMechanisms("ITEM001", threads);

        System.out.println("\n================================ PERFORMANCE COMPARISON TABLE ================================");
        System.out.printf("| %-15s | %-7s | %-7s | %-7s | %-14s | %-10s | %-10s | %-12s |\n", "LockMechanism", "Threads", "Success", "Failed", "Overselling", "Time(ms)", "TPS(req/s)", "vs Baseline");
        System.out.println("------------------------------------------------------------------");
        for (SimulationResult r : results) {
            System.out.println(r.toAsciiTableRow());
        }

        System.out.println("=================================================================");
        
    }
    
    private void printSingleResult(SimulationResult r) {
        System.out.println("\n--- SIMULATION RESULT ---");
        System.out.println("Lock Mechanism: " + r.getLockMechanism());
        System.out.println("Threads: " + r.getThreads());
        System.out.println("Success Count: " + r.getSuccessCount());
        System.out.println("Failed Count: " + r.getFailedCount());
        System.out.println("Negative Stock (Overselling): " + r.getNegativeStockCount() + (r.getNegativeStockCount() > 0 ? " -> [OVERSELLING DETECTED!]" : " -> [SAFE - NO OVERSELLING]"));
        System.out.printf("Throughput (TPS): %.2f req/s\n", r.getTps());
    }

    public static void main(String[] args) {
        new SimulatorView().start();
    }
}
