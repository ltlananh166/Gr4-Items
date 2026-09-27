package view;

import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseEx;

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
            System.out.println("Loi: Khong duoc de trong. Vui long nhap lai.");
        }
    }

    public String readStringPattern(String prompt, String regex, String errorMsg) {
        while (true) {
            String input = readStringRequired(prompt);
            if (input.matches(regex)) {
                return input;
            }
            System.out.println("Loi: " + errorMsg);
        }
    }

    public String readDateTime(String prompt) {
        While (true) {
            System.out.print(prompt + " (dinh dang: dd/MM/yyyy HH:mm): ");
            String input = scanner.nextLine().trim();
            try {
                LocalDateTime.parse(input, DATE_TIME_FORMATTER);
                return input;
            } catch (DateTimeParseException e) {
                System.out.println("Loi: Sai dinh dang ngay gio. Vui long nhap lai!");
            }
        }
    }

    public int readInt(String prompt, int defaultValue) {
        while (true) {
            System.out.print(prompt + " (Mac dinh: " + defaultValue + "): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(input);
            }catch (NumberFormatException e) {
                System.out.println("Loi: Vui long nhap mot so nguyen hop le!");
            }
        }
    }

    public readIntRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Intger.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Loi: Vui long nhap mot so nguyen hop le!");
            }
        }
    }

    public int readIntMin(String prompt, int min, String errorMsg) {
        while (true) {
            int value = readIntRequired(prompt);
            if (value >= min) {
                return value;
            }
            System.out.println("Loi: " + errorMsg);
        }
    }

    public int readIntMinMax(String prompt, int min, int max, String errorMsg) {
        while (true) {
            int value = readIntRequired(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("Loi: " + errorMsg);
        }
    }

    public double readDouble(String prompt, double defaultValue) {
        while (true) {
            System.out.print(promt + " (Mac dinh: " + defaultValue + "): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaultValue;
            }
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Loi: Vui long nhap so thuc hop le!");
            }
        }
    }

}