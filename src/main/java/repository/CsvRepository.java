
package repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CsvRepository<T> {

    private static final ConcurrentHashMap<Path, ReentrantReadWriteLock>
            LOCKS = new ConcurrentHashMap<>();

    private final Class<T> type;
    private final Path filePath;
    private final String fileName;
    private final String header;
    private final ReentrantReadWriteLock rwLock;

    public CsvRepository(Class<T> type, String fileName, String header) {
        this(type, "data", fileName, header);
    }

    public CsvRepository(
            Class<T> type,
            String dataDirectory,
            String fileName,
            String header) {

        if (type == null || dataDirectory == null
                || fileName == null || header == null) {
            throw new IllegalArgumentException(
                    "Repository arguments must not be null");
        }

        this.type = type;
        this.fileName = fileName;
        this.header = header;

        Path directory = Paths.get(dataDirectory)
                .toAbsolutePath().normalize();

        this.filePath = directory.resolve(fileName).normalize();

        if (!this.filePath.startsWith(directory)) {
            throw new IllegalArgumentException(
                    "Invalid CSV file path: " + fileName);
        }

        this.rwLock = LOCKS.computeIfAbsent(
                this.filePath, key -> new ReentrantReadWriteLock());

        rwLock.writeLock().lock();
        try {
            Files.createDirectories(directory);

            if (!Files.exists(filePath)) {
                try (BufferedWriter writer = Files.newBufferedWriter(
                        filePath, StandardCharsets.UTF_8)) {
                    writer.write(header);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot initialize CSV file: " + filePath, e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public List<T> findAll() {
        rwLock.readLock().lock();
        try {
            return readAllUnlocked();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public T findById(String id) {
        if (id == null) {
            return null;
        }

        rwLock.readLock().lock();
        try {
            for (T object : readAllUnlocked()) {
                Object objectId = getIdValue(object);

                if (id.equals(String.valueOf(objectId))) {
                    return object;
                }
            }
            return null;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void save(T object) {
        if (object == null) {
            throw new IllegalArgumentException("Object must not be null");
        }

        rwLock.writeLock().lock();
        try {
            List<T> objects = readAllUnlocked();
            objects.add(object);
            writeAllUnlocked(objects);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void saveAll(List<T> objects) {
        if (objects == null) {
            throw new IllegalArgumentException("List must not be null");
        }

        rwLock.writeLock().lock();
        try {
            writeAllUnlocked(objects);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Updates an existing object only if its persisted version matches
     * expectedVersion. The caller supplies the updated object.
     */
    public boolean update(T updatedObject, int expectedVersion) {
        if (updatedObject == null) {
            throw new IllegalArgumentException(
                    "Updated object must not be null");
        }

        rwLock.writeLock().lock();
        try {
            List<T> objects = readAllUnlocked();
            Object updatedId = getIdValue(updatedObject);

            if (updatedId == null) {
                return false;
            }

            for (int i = 0; i < objects.size(); i++) {
                T current = objects.get(i);
                Object currentId = getIdValue(current);

                if (updatedId.equals(currentId)) {
                    int currentVersion = getVersion(current);

                    if (currentVersion != expectedVersion) {
                        return false;
                    }

                    objects.set(i, updatedObject);
                    writeAllUnlocked(objects);
                    return true;
                }
            }

            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public String getFileName() {
        return fileName;
    }

    public Path getFilePath() {
        return filePath;
    }

    public ReentrantReadWriteLock getRwLock() {
        return rwLock;
    }

    /**
     * Caller must already hold the appropriate lock.
     */
    protected List<T> readAllUnlocked() {
        List<T> result = new ArrayList<>();

        if (!Files.exists(filePath)) {
            return result;
        }

        try (BufferedReader reader = Files.newBufferedReader(
                filePath, StandardCharsets.UTF_8)) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                return result;
            }
            List<String> columns = parseCsvLine(headerLine);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> values = parseCsvLine(line);

                T object = type.getDeclaredConstructor().newInstance();

                for (int i = 0;
                        i < columns.size() && i < values.size(); i++) {
                    setField(object, columns.get(i).trim(), values.get(i));
                }

                result.add(object);
            }

            return result;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot read CSV file: " + filePath, e);
        }
    }

    /**
     * Caller must already hold the write lock.
     * Writes a temporary file before replacing the CSV destination.
     */
    protected void writeAllUnlocked(List<T> objects) {
        Path tempFile = null;

        try {
            Path directory = filePath.getParent();

            tempFile = Files.createTempFile(
                    directory, fileName, ".tmp");

            try (BufferedWriter writer = Files.newBufferedWriter(
                    tempFile, StandardCharsets.UTF_8)) {

                writer.write(header);
                writer.newLine();

                String[] columns = header.split(",", -1);

                for (T object : objects) {
                    List<String> values = new ArrayList<>();

                    for (String column : columns) {
                        Object value = getField(object, column.trim());
                        values.add(escapeCsv(
                                value == null ? "" : value.toString()));
                    }

                    writer.write(String.join(",", values));
                    writer.newLine();
                }
            }

            try {
                Files.move(tempFile, filePath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveException) {
                Files.move(tempFile, filePath,
                        StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot write CSV file: " + filePath, e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // Temporary file cleanup is best effort.
                }
            }
        }
    }

   

private Object getIdValue(T object) {
    if (object instanceof model.BaseEntity) {
        return ((model.BaseEntity) object).getId();
    }

    for (String name : new String[]{
            "id", "itemId", "productId", "voucherId",
            "orderDetailId", "orderId", "customerId",
            "eventId", "detailId", "transactionId"}) {
        try {
            Field field = findField(name);
            field.setAccessible(true);
            return field.get(object);
        } catch (NoSuchFieldException ignored) {
            // Try the next possible ID field.
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Cannot read ID in " + type.getSimpleName(), e);
        }
    }

    throw new IllegalStateException(
            "No supported ID field found in " + type.getSimpleName());
}



    private int getVersion(T object) {
        try {
            Field field = findField("version");
            field.setAccessible(true);
            Object value = field.get(object);

            if (value instanceof Number) {
                return ((Number) value).intValue();
            }

            throw new IllegalStateException(
                    "Version field must be numeric in "
                    + type.getSimpleName());

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Cannot read version in " + type.getSimpleName(), e);
        }
    }

    private Object getField(T object, String fieldName) {
        try {
            Field field = findField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot read field '" + fieldName
                    + "' in " + type.getSimpleName(), e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setField(T object, String fieldName, String value) {
        String mappedFieldName = mapCsvFieldName(object, fieldName, value);
        if (mappedFieldName == null) {
            return;
        }
        try {
            Field field = findField(mappedFieldName);
            CsvValueConverter.assign(object, field, value);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot set field '" + fieldName
                    + "' in " + type.getSimpleName(), e);
        }
    }

    private String mapCsvFieldName(Object object, String fieldName, String value) {
        if (object instanceof model.RegularProduct && "brand".equals(fieldName)) {
            return null;
        }
        if (object instanceof model.FlashSaleEvent) {
            if ("name".equals(fieldName)) {
                return "title";
            }
            if ("unlockTime".equals(fieldName) && (value == null || value.isBlank())) {
                return null;
            }
        }
        if (object instanceof model.Order) {
            if ("customerName".equals(fieldName) || "orderDate".equals(fieldName)
                    || "eventId".equals(fieldName)) {
                return null;
            }
        }
        if (object instanceof model.OrderDetail) {
            if ("detailId".equals(fieldName)) {
                return "orderDetailId";
            }
            if ("subtotal".equals(fieldName)) {
                return null;
            }
        }
        return fieldName;
    }

    private Field findField(String fieldName)
            throws NoSuchFieldException {

        Class<?> current = type;

        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }

        throw new NoSuchFieldException(fieldName);
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (insideQuotes
                        && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (c == ',' && !insideQuotes) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(c);
            }
        }

        if (insideQuotes) {
            throw new IllegalArgumentException(
                    "Malformed CSV line: unmatched quotation mark");
        }

        values.add(value.toString());
        return values;
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}
