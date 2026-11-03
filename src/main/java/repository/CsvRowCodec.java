package repository;

import java.util.ArrayList;
import java.util.List;

/** Thoát ký tự theo kiểu RFC 4180 cho tập con cú pháp mà dự án sử dụng. */
public final class CsvRowCodec {
    private CsvRowCodec() {
    }

    public static String[] split(String line) {
        List<String> values = new ArrayList<>();
        boolean quoted = false;
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unclosed CSV quote");
        }
        values.add(current.toString());
        return values.toArray(new String[0]);
    }

    public static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
 