
package repository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Converts a CSV token to the declared Java field type. */
public final class CsvValueConverter {

    private CsvValueConverter() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void assign(
            Object entity,
            Field field,
            String rawValue
    ) throws IllegalAccessException {

        String value = rawValue == null ? "" : rawValue.trim();
        Class<?> fieldType = field.getType();

        field.setAccessible(true);

        if (fieldType == String.class) {
            field.set(entity, value);

        } else if (fieldType == int.class || fieldType == Integer.class) {
            field.set(entity, value.isEmpty() ? 0 : Integer.parseInt(value));

        } else if (fieldType == long.class || fieldType == Long.class) {
            field.set(entity, value.isEmpty() ? 0L : Long.parseLong(value));

        } else if (fieldType == double.class || fieldType == Double.class) {
            field.set(entity, value.isEmpty() ? 0.0 : Double.parseDouble(value));

        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            field.set(entity, Boolean.parseBoolean(value));

        } else if (fieldType == BigDecimal.class) {
            field.set(entity, value.isEmpty()
                    ? BigDecimal.ZERO
                    : new BigDecimal(value));

        } else if (fieldType == LocalDateTime.class) {
            if (value.isEmpty()) {
                field.set(entity, null);
            } else {
                try {
                    field.set(entity, LocalDateTime.parse(value));
                } catch (DateTimeParseException exception) {
                    field.set(entity, LocalDateTime.parse(value,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
            }

        } else if (fieldType.isEnum()) {
            if (!value.isEmpty()) {
                field.set(entity,
                        Enum.valueOf((Class<Enum>) fieldType,
                                value.toUpperCase()));
            }

        } else {
            throw new IllegalArgumentException(
                    "Unsupported CSV field type: " + fieldType.getName());
        }
    }
}
