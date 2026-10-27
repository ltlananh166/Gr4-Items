package service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import model.Customer;
import model.enums.CustTier;
import security.PasswordSecurity;

public class DataGeneratorService {
    private static final int PRODUCT_COUNT = 5_000;
    private static final int CUSTOMER_COUNT = 2_000;
    private static final int VOUCHER_COUNT = 10;
    private static final int EVENT_COUNT = 10;
    private static final int FLASH_ITEM_COUNT = 500;
    private static final int ORDER_COUNT = 2_500;
    private static final int ORDER_DETAIL_COUNT = 2_500;
    private static final DateTimeFormatter EVENT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CUSTOMER_HEADER =
            "customerId,name,email,phone,address,avatarUrl,tier,status,password,walletBalance";
    private static final String VOUCHER_HEADER =
            "voucherId,code,discountType,discountAmount,minOrderValue,maxDiscountValue,validFrom,validTo,status";
    private static final String[] PRODUCT_NAMES = {
        "Tai nghe", "Ao thun", "Chuot gaming", "Ban phim", "Loa",
        "Dong ho", "Giay", "Balo", "Sac du phong", "Webcam",
        "USB hub", "Cap sac", "Op lung", "May anh", "Tablet",
        "Laptop", "Man hinh", "Quat mini", "Kinh mat", "Tay cam"
    };
    private static final String[] BRANDS = {
        "Samsung", "Xiaomi", "Apple", "Sony", "JBL",
        "Logitech", "Razer", "Asus", "HP", "Anker"
    };
    private static final String[] CATEGORIES = {
        "Electronics", "Fashion", "Home", "Accessories", "Gaming"
    };
    private static final String[] FIRST_NAMES = {
        "Nguyen", "Tran", "Le", "Pham", "Hoang",
        "Phan", "Vu", "Vo", "Dang", "Bui"
    };
    private static final String[] LAST_NAMES = {
        "Anh", "Binh", "Cuong", "Dung", "Hieu",
        "Hung", "Khanh", "Linh", "Minh", "Nam",
        "Phuc", "Quang", "Son", "Thanh", "Trung"
    };
    private static final String[] VOUCHER_CODES = {
        "WELCOME10", "FREESHIP", "GOLDVIP", "FLASH20", "CHAOHE",
        "DATA10", "DATA15", "DATA20", "DATA25", "DATA30"
    };

    private final Path dataDirectory;

    public DataGeneratorService() {
        this(Path.of("data"));
    }

    public DataGeneratorService(String dataDirectory) {
        this(Path.of(dataDirectory));
    }

    public DataGeneratorService(Path dataDirectory) {
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
    }

    public Map<String, Integer> generateAll() throws IOException {
        Files.createDirectories(dataDirectory);
        Map<String, Integer> counts = new LinkedHashMap<>();

        counts.put("products.csv", generateProducts());
        List<Customer> customers = generateCustomers();
        counts.put("customers.csv", countDataRows("customers.csv"));
        counts.put("vouchers.csv", generateVouchers());
        counts.put("flash_events.csv", generateFlashEvents());
        counts.put("flash_items.csv", generateFlashItems());
        counts.put("orders.csv", generateOrders(customers));
        counts.put("order_details.csv", generateOrderDetails());
        counts.put("transactions.csv", ensureTransactionsFile());

        int total = counts.entrySet().stream()
                .filter(entry -> !"transactions.csv".equals(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .sum();
        counts.put("TOTAL", total);
        return counts;
    }

    private int generateProducts() throws IOException {
        Path file = dataDirectory.resolve("products.csv");
        if (!Files.exists(file)) {
            writeCsv(file, "productId,name,brand,category,price,stock,description,version", writer -> {
                for (int i = 1; i <= PRODUCT_COUNT; i++) {
                    String brand = pick(BRANDS);
                    String name = brand + " " + pick(PRODUCT_NAMES) + " " + pick(BRANDS);
                    int price = randomInt(50_000, 5_000_000);
                    int stock = randomInt(10, 500);
                    writeRow(writer, String.format(Locale.ROOT,
                            "P-%05d,%s,%s,%s,%d,%d,Quality sample product,0",
                            i, csv(name), brand, pick(CATEGORIES), price, stock));
                }
            });
        }
        return countDataRows("products.csv");
    }

    private List<Customer> generateCustomers() throws IOException {
        Path file = dataDirectory.resolve("customers.csv");
        List<String> lines = Files.exists(file)
                ? Files.readAllLines(file, StandardCharsets.UTF_8)
                : new ArrayList<>();
        if (lines.isEmpty()) {
            lines.add(CUSTOMER_HEADER);
        } else if (!CUSTOMER_HEADER.equals(lines.get(0))) {
            throw new IOException("Unexpected customers.csv header; existing customer data was not changed.");
        }

        List<Customer> customers = new ArrayList<>();
        Set<String> emails = new LinkedHashSet<>();
        int generatedCount = 0;
        int maxId = 0;
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).trim().isEmpty()) {
                continue;
            }
            Customer customer;
            try {
                customer = Customer.fromCsvLine(lines.get(i));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid existing customer row at line " + (i + 1)
                        + "; existing customer data was not changed.", exception);
            }
            if (customer == null) {
                continue;
            }
            customers.add(customer);
            if (customer.getEmail() != null) {
                emails.add(customer.getEmail().toLowerCase(Locale.ROOT));
                if (customer.getEmail().toLowerCase(Locale.ROOT).endsWith("@seed.gr4.local")) {
                    generatedCount++;
                }
            }
            String id = customer.getCustomerId();
            if (id != null && id.matches("C-\\d+")) {
                maxId = Math.max(maxId, Integer.parseInt(id.substring(2)));
            }
        }

        int rowsToAdd = Math.max(0, CUSTOMER_COUNT - generatedCount);
        if (rowsToAdd > 0) {
            String passwordHash = PasswordSecurity.hash("generated-sample-" + java.util.UUID.randomUUID());
            for (int i = 1; i <= rowsToAdd; i++) {
                int id = ++maxId;
                String firstName = pick(FIRST_NAMES);
                String lastName = pick(LAST_NAMES);
                String name = firstName + " Van " + lastName;
                String email = String.format(Locale.ROOT, "seed%04d@seed.gr4.local",
                        generatedCount + i);
                while (emails.contains(email.toLowerCase(Locale.ROOT))) {
                    email = String.format(Locale.ROOT, "seed%04d-%d@seed.gr4.local",
                            generatedCount + i, id);
                }
                emails.add(email.toLowerCase(Locale.ROOT));
                String tier = randomTier();
                String phone = String.format(Locale.ROOT, "09%08d", randomInt(0, 99_999_999));
                String row = String.join(",", String.format(Locale.ROOT, "C-%05d", id),
                        csv(name), email, phone, "Ha Noi", "", tier, "ACTIVE",
                        csv(passwordHash), "10000000.0");
                lines.add(row);
                customers.add(new Customer(String.format(Locale.ROOT, "C-%05d", id),
                        name, email, phone, "Ha Noi", "", CustTier.fromValue(tier),
                        "ACTIVE", passwordHash, 10_000_000.0));
            }
            writeLinesAtomically(file, lines);
        }
        return customers;
    }

    private int generateVouchers() throws IOException {
        Path file = dataDirectory.resolve("vouchers.csv");
        List<String> lines = Files.exists(file)
                ? Files.readAllLines(file, StandardCharsets.UTF_8)
                : new ArrayList<>();
        if (lines.isEmpty()) {
            lines.add(VOUCHER_HEADER);
        } else if (!VOUCHER_HEADER.equals(lines.get(0))) {
            throw new IOException("Unexpected vouchers.csv header; existing voucher data was not changed.");
        }

        Set<String> existingCodes = new LinkedHashSet<>();
        Set<String> existingIds = new LinkedHashSet<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] columns = line.split(",", -1);
            if (columns.length != 9) {
                throw new IOException("Invalid voucher row at line " + (i + 1)
                        + "; existing voucher data was not changed.");
            }
            existingIds.add(columns[0]);
            existingCodes.add(columns[1].toUpperCase(Locale.ROOT));
        }

        int nextId = 1;
        for (String code : VOUCHER_CODES) {
            if (existingCodes.contains(code)) {
                continue;
            }
            String id;
            do {
                id = String.format(Locale.ROOT, "V-GEN-%03d", nextId++);
            } while (existingIds.contains(id));
            String type = code.equals("FREESHIP") || code.equals("GOLDVIP")
                    ? "FIXED" : "PERCENTAGE";
            int amount = "FIXED".equals(type) ? 30_000 + nextId * 1_000 : 10 + nextId;
            int maximum = "FIXED".equals(type) ? amount : 50_000 + nextId * 1_000;
            int minimum = nextId * 10_000;
            lines.add(String.join(",", id, code, type, Integer.toString(amount),
                    Integer.toString(minimum), Integer.toString(maximum), "", "", "ACTIVE"));
            existingIds.add(id);
            existingCodes.add(code);
        }
        if (!Files.exists(file) || lines.size() != Files.readAllLines(file, StandardCharsets.UTF_8).size()) {
            writeLinesAtomically(file, lines);
        }
        return countDataRows("vouchers.csv");
    }

    private int generateFlashEvents() throws IOException {
        Path file = dataDirectory.resolve("flash_events.csv");
        if (!Files.exists(file)) {
            LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
            writeCsv(file, "eventId,name,startTime,endTime,status,unlockTime", writer -> {
                for (int i = 1; i <= EVENT_COUNT; i++) {
                    LocalDateTime start = i == 1 ? now.minusMinutes(5)
                            : now.plusDays(i - 1L).withHour(9 + i % 10).withMinute(0);
                    LocalDateTime end = start.plusMinutes(60L + (i % 3) * 30L);
                    String status = i == 1 ? "ACTIVE" : "UPCOMING";
                    writeRow(writer, String.format(Locale.ROOT, "EV-%03d,Flash Sale %02d,%s,%s,%s,",
                            i, i, start.format(EVENT_TIME_FORMAT), end.format(EVENT_TIME_FORMAT), status));
                }
            });
        }
        return countDataRows("flash_events.csv");
    }

    private int generateFlashItems() throws IOException {
        Path file = dataDirectory.resolve("flash_items.csv");
        if (!Files.exists(file)) {
            writeCsv(file,
                    "itemId,productId,eventId,productName,originalPrice,salePrice,initialStock,soldQty,remainingStock,version",
                    writer -> {
                        for (int i = 1; i <= FLASH_ITEM_COUNT; i++) {
                            int originalPrice = randomInt(100_000, 3_000_000);
                            int salePrice = Math.max(1,
                                    (int) (originalPrice * (0.3 + ThreadLocalRandom.current().nextDouble() * 0.4)));
                            int stock = randomInt(5, 200);
                            writeRow(writer, String.format(Locale.ROOT,
                                    "FI-%05d,P-%05d,EV-%03d,%s %s,%d,%d,%d,0,%d,0",
                                    i, randomInt(1, PRODUCT_COUNT), randomInt(1, EVENT_COUNT),
                                    pick(BRANDS), pick(PRODUCT_NAMES), originalPrice, salePrice, stock, stock));
                        }
                    });
        }
        return countDataRows("flash_items.csv");
    }

    private int generateOrders(List<Customer> customers) throws IOException {
        Path file = dataDirectory.resolve("orders.csv");
        if (!Files.exists(file)) {
            writeCsv(file, "orderId,customerId,customerName,orderDate,totalAmount,status,eventId", writer -> {
                for (int i = 1; i <= ORDER_COUNT; i++) {
                    Customer customer = customers.get(ThreadLocalRandom.current().nextInt(customers.size()));
                    String orderDate = LocalDate.now().minusDays(randomInt(0, 29)).toString();
                    int total = randomInt(100_000, 5_000_000);
                    String status = ThreadLocalRandom.current().nextInt(10) == 0 ? "FAILED" : "SUCCESS";
                    writeRow(writer, String.format(Locale.ROOT, "O-%05d,%s,%s,%s,%d,%s,EV-%03d",
                            i, customer.getCustomerId(), csv(customer.getName()), orderDate, total,
                            status, randomInt(1, EVENT_COUNT)));
                }
            });
        }
        return countDataRows("orders.csv");
    }

    private int generateOrderDetails() throws IOException {
        Path file = dataDirectory.resolve("order_details.csv");
        if (!Files.exists(file)) {
            writeCsv(file, "detailId,orderId,productId,quantity,unitPrice,subtotal", writer -> {
                for (int i = 1; i <= ORDER_DETAIL_COUNT; i++) {
                    int quantity = randomInt(1, 2);
                    int unitPrice = randomInt(50_000, 3_000_000);
                    writeRow(writer, String.format(Locale.ROOT, "D-%06d,O-%05d,P-%05d,%d,%d,%d",
                            i, i, randomInt(1, PRODUCT_COUNT), quantity, unitPrice,
                            quantity * unitPrice));
                }
            });
        }
        return countDataRows("order_details.csv");
    }

    private int ensureTransactionsFile() throws IOException {
        Path file = dataDirectory.resolve("transactions.csv");
        if (!Files.exists(file)) {
            writeCsv(file,
                    "transactionId,orderId,customerId,itemId,quantity,status,message,timestamp",
                    writer -> { });
        }
        return countDataRows("transactions.csv");
    }

    private int countDataRows(String fileName) throws IOException {
        List<String> lines = Files.readAllLines(dataDirectory.resolve(fileName), StandardCharsets.UTF_8);
        return (int) lines.stream().filter(line -> !line.trim().isEmpty()).count() - 1;
    }

    private static String randomTier() {
        int value = randomInt(1, 100);
        if (value > 95) {
            return "DIAMOND";
        }
        if (value > 80) {
            return "GOLD";
        }
        if (value > 60) {
            return "SILVER";
        }
        return "STANDARD";
    }

    private static String pick(String[] values) {
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    private static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static String csv(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static void writeCsv(Path file, String header, CsvWriter rows) throws IOException {
        writeAtomically(file, writer -> {
            writer.write(header);
            writer.newLine();
            rows.writeRows(writer);
        });
    }

    private static void writeRow(BufferedWriter writer, String row) throws IOException {
        writer.write(row);
        writer.newLine();
    }

    private static void writeLinesAtomically(Path file, List<String> lines) throws IOException {
        writeAtomically(file, writer -> {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        });
    }

    private static void writeAtomically(Path file, CsvWriter content) throws IOException {
        Path temporary = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                content.writeRows(writer);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @FunctionalInterface
    private interface CsvWriter {
        void writeRows(BufferedWriter writer) throws IOException;
    }
}
