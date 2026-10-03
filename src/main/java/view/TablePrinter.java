package view;

import java.util.ArrayList;
import java.util.List;

public class TablePrinter {
    private String[] headers;
    private List<String[]> rows;
    private int[] columnWidths;

    public TablePrinter(String... headers) {
        this.headers = headers;
        this.rows = new ArrayList<>();
        this.columnWidths = new int[headers.length];

        for (int i = 0; i < headers.length; i++) {
            columnWidths[i] = headers[i].length();
        }
    }

    public void addRow(String... rowData) {
        if (rowData.length != headers.length) {
            throw new IllegalArgumentException("Lỗi: Số lượng cột dữ liệu không khớp với Tiêu đề!");
        }

        for (int i = 0; i < rowData.length; i++) {
            if (rowData[i] != null && rowData[i].length() > columnWidths[i]) {
                columnWidths[i] = rowData[i].length();
            }
        }
        rows.add(rowData);
    }

    public void print() {
        printLine();
        printRow(headers);
        printLine();
        for (String[] row : rows) {
            printRow(row);
        }
        printLine();
    }

    public void printLine() {
        System.out.print("+");
        for (int width : columnWidths) {
            for (int i = 0; i < width + 2; i++) {
                System.out.print("-");
            }
            System.out.print("+");
        }
        System.out.println();
    }

    public void printRow(String[] rowData) {
        System.out.print("|");
        for (int i = 0; i < rowData.length; i++) {
            String cellData = rowData[i] == null ? "" : rowData[i];
            String format = " %- " + columnWidths[i] + "s |";
            System.out.print(format, cellData);
        }
        System.out.println();
    }
}