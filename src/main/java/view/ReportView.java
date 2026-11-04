package view;

import java.util.Map;

public class ReportView {
    public void displayDataGenerationResult(Map<String, Integer> result) {
        System.out.println("Data generated:");
        result.forEach((file, count) -> System.out.printf("- %s: %d rows%n", file, count));
    }
}

