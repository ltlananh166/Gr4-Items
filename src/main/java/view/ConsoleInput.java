package view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class ConsoleInput {
    private final Scanner scanner;
    private final  DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public String readStringRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("Error: Cannot be left empty. Please try again.");
        }
    }

    public String readStringPattern(String prompt, String regex, String errorMsg) {
        while (true) {
            String input = readStringRequired(prompt);
            if (input.matches(regex)) {
                return input;
            }
            System.out.println("Error: " + errorMsg);
        }
    }

    public String readDateTime(String prompt) {
        while (true) {
            System.out.print(prompt + " ((format: dd/MM/yyyy HH:mm): ");
            String input = scanner.nextLine().trim();
            try {
                LocalDateTime.parse(input, DATE_TIME_FORMATTER);
                return input;
            } catch (DateTimeParseException e) {
                System.out.println("Error: Invalid date/time format. Please try again!");
            }
        }
    }

    public int readInt(String prompt, int defaultValue) {
        while (true) {
            System.out.print(prompt + " (Default: " + defaultValue + "): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(input);
            }catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid integer!");
            }
        }
    }

    public int readIntRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid integer!");
            }
        }
    }

    public int readIntMin(String prompt, int min, String errorMsg) {
        while (true) {
            int value = readIntRequired(prompt);
            if (value >= min) {
                return value;
            }
            System.out.println("Error: " + errorMsg);
        }
    }

    public int readIntMinMax(String prompt, int min, int max, String errorMsg) {
        while (true) {
            int value = readIntRequired(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("Error: " + errorMsg);
        }
    }

    public double readDouble(String prompt, double defaultValue) {
        while (true) {
            System.out.print(prompt + " (Mac dinh: " + defaultValue + "): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaultValue;
            }
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid decimal number!");
            }
        }
    }

}