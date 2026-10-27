package service;

import model.Customer;
import model.enums.CustTier;
import repository.CsvRowCodec;
import security.PasswordSecurity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class DataGeneratorService {
    private static final DateTimeFormatter EVENT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String CUSTOMER_HEADER =
            "customerId,name,email,phone,address,avatarUrl,tier,status,password,walletBalance";
    private static final String TRANSACTION_HEADER =
            "transactionId,orderId,customerId,itemId,quantity,status,message,timestamp";

    private static final String[] PRODUCT_PREFIXES = {
            "Tai Nghe", "Ao Thun", "Chuot Gaming", "Ban Phim", "Loa",
            "Dong Ho", "Giay", "Balo", "Sac Du Phong", "Kinh Mat",
            "Quat Mini", "Webcam", "USB Hub", "Cap Sac", "Op Lung",
            "Ong Nhom", "May Anh", "Tablet", "Laptop", "Monitor"
    };
    private static final String[] PRODUCT_SUFFIXES = {
            "Pro Max", "Ultra", "Lite", "Plus", "Premium",
            "V2", "Classic", "Sport", "Mini", "XL"
    };
    private static final String[] BRANDS = {
            "Samsung", "Xiaomi", "Apple", "Sony", "JBL",
            "Logitech", "Razer", "Asus", "HP", "Anker"
    };
    private static final String[] FIRST_NAMES = {
            "Nguyen", "Tran", "Le", "Pham", "Hoang",
            "Phan", "Vu", "Vo", "Dang", "Bui"
    };
    private static final String[] LAST_NAMES = {
            "Anh", "Binh", "Cuong", "Dung", "Hieu", "Hung", "Khanh", "Linh",
            "Minh", "Nam", "Phuc", "Quang", "Son", "Thanh", "Trung"
    };

    private final Path dataDirectory;

    public DataGeneratorService() {
        this("data");
    }

    public DataGeneratorService(String dataDirectory) {
        this.dataDirectory = Paths.get(dataDirectory);
    }

    public Map<String, Integer> generateAll() throws IOException {
        Files.createDirectories(dataDirectory);

        Map<String, Customer> existingCustomers = readExistingCustomers();
        Map<String, String> customerNames = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("products.csv", generateProducts(5_000));
        counts.put("customers.csv", generateCustomers(2_000, existingCustomers, customerNames));
        counts.put("vouchers.csv", generateVouchers(10));
        counts.put("flash_events.csv", generateFlashEvents(10));
        counts.put("flash_items.csv", generateFlashItems(500));
        counts.put("orders.csv", generateOrders(2_500, customerNames));
        counts.put("order_details.csv", generateOrderDetails(2_500));
        counts.put("transactions.csv", ensureTransactionsFile());

        int total = counts.entrySet().stream()
                .filter(entry -> !"transactions.csv".equals(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .sum();
        counts.put("TOTAL", total);
        return counts;
    }

    private int generateProducts(int count) throws IOException {
        try (AtomicCsvFile csv = newAtomicFile("products.csv")) {
            BufferedWriter writer = csv.writer();
            writer.write("productId,name,brand,category,price,stock,description,version");
            writer.newLine();
            for (int i = 1; i <= count; i++) {
                String prefix = pick(PRODUCT_PREFIXES);
                String brand = pick(BRANDS);
                writer.write(String.format(Locale.ROOT,
                        "P-%05d,%s %s %s,%s,%s,%d,%d,San pham chat luong cao,0",
                        i, brand, prefix, pick(PRODUCT_SUFFIXES), brand, prefix,
                        randomInt(50, 5_000) * 1_000, randomInt(10, 500)));
                writer.newLine();
            }
            csv.commit();
        }
        return count;
    }

    private int generateCustomers(
            int count,
            Map<String, Customer> existingCustomers,
            Map<String, String> customerNames) throws IOException {
        String passwordHash = PasswordSecurity.hash(UUID.randomUUID().toString());
        try (AtomicCsvFile csv = newAtomicFile("customers.csv")) {
            BufferedWriter writer = csv.writer();
            writer.write(CUSTOMER_HEADER);
            writer.newLine();
            for (int i = 1; i <= count; i++) {
                String id = String.format(Locale.ROOT, "C-%05d", i);
                Customer customer = existingCustomers.get(id);
                if (customer == null) {
                    String firstName = pick(FIRST_NAMES);
                    String lastName = pick(LAST_NAMES);
                    String name = firstName + " Van " + lastName;
                    String tier = randomTier();
                    customer = new Customer(id, name,
                            (firstName + lastName + i + "@gmail.com").toLowerCase(Locale.ROOT),
                            "09" + randomInt(10_000_000, 99_999_999),
                            "Ha Noi", "", CustTier.fromValue(tier), "ACTIVE",
                            passwordHash, 10_000_000.0);
                }
                customerNames.put(id, customer.getName());
                writer.write(customer.toCsvLine());
                writer.newLine();
            }

            for (Customer customer : existingCustomers.values()) {
                if (!customerNames.containsKey(customer.getCustomerId())) {
                    customerNames.put(customer.getCustomerId(), customer.getName());
                    writer.write(customer.toCsvLine());
                    writer.newLine();
                }
            }
            csv.commit();
        }
        return customerNames.size();
    }

    private int generateVouchers(int count) throws IOException {
        try (AtomicCsvFile csv = newAtomicFile("vouchers.csv")) {
            BufferedWriter writer = csv.writer();
            writer.write("voucherId,code,type,value,maxDiscount,minOrderAmount,remainingUses");
            writer.newLine();
            writer.write("V-00001,SHOPEE10,PERCENTAGE,10,50000,0,1000");
            writer.newLine();
            writer.write("V-00002,FREESHIP,FIXED,30000,30000,100000,1000");
            writer.newLine();
            writer.write("V-00003,GOLDVIP,FIXED,50000,50000,150000,500");
            writer.newLine();
            writer.write("V-00004,DIAMONDSALE,PERCENTAGE,20,100000,200000,500");
            writer.newLine();
            writer.write("V-00005,CHAOHE,PERCENTAGE,15,30000,50000,2000");
            writer.newLine();
            for (int i = 6; i <= count; i++) {
                String id = String.format(Locale.ROOT, "V-%05d", i);
                String code = String.format(Locale.ROOT, "SALE%02d", i * 5);
                boolean percentage = randomInt(0, 1) == 1;
                String type = percentage ? "PERCENTAGE" : "FIXED";
                int value = percentage ? randomInt(5, 25) : randomInt(10, 50) * 1_000;
                int maxDiscount = percentage ? randomInt(20, 100) * 1_000 : value;
                writer.write(String.format(Locale.ROOT, "%s,%s,%s,%d,%d,%d,%d",
                        id, code, type, value, maxDiscount,
                        randomInt(0, 150) * 1_000, randomInt(50, 500)));
                writer.newLine();
            }
            csv.commit();
        }
        return count;
    }

    private int generateFlashEvents(int count) throws IOException {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        try (AtomicCsvFile csv = newAtomicFile("flash_events.csv")) {
            BufferedWriter writer = csv.writer();
            writer.write("eventId,name,startTime,endTime,status,unlockTime");
            writer.newLine();
            for (int i = 1; i <= count; i++) {
                LocalDateTime start = i == 1
                        ? now.minusMinutes(5)
                        : now.plusDays(i - 1L).withHour(9 + i % 10).withMinute(0);
                LocalDateTime end = start.plusMinutes(60L + i % 3 * 30L);
                String status = i == 1 ? "ACTIVE" : "UPCOMING";
                writer.write(String.format(Locale.ROOT, "EV-%03d,Flash Sale %02d,%s,%s,%s,",
                        i, i, start.format(EVENT_TIME_FORMAT), end.format(EVENT_TIME_FORMAT), status));
                writer.newLine();
            }
            csv.commit();
        }
        return count;
    }

    private int generateFlashItems(int count) throws IOException {
        try (AtomicCsvFile csv = newAtomicFile("flash_items.csv")) {
            BufferedWriter writer = csv.writer();
            writer.write("itemId,productId,eventId,productName,originalPrice,salePrice,"
                    + "initialStock,soldQty,remainingStock,version");
            writer.newLine();
            for (int i = 1; i <= count; i++) {
                int originalPrice = randomInt(100, 3_000) * 1_000;
                int salePrice = (int) (originalPrice * (0.3 + Math.random() * 0.4));
                int stock = randomInt(5, 200);
                writer.write(String.format(Locale.ROOT,
                        "FI-%05d,P-%05d,EV-%03d,%s %s %s,%d,%d,%d,0,%d,0",
                        i, randomInt(1, 5_000), randomInt(1, 10),
                        pick(BRANDS), pick(PRODUCT_PREFIXES), pick(PRODUCT_SUFFIXES),
                        originalPrice, salePrice, stock, stock));
                writer.newLine();
            }
            csv.commit();
        }
        return count;
    }

    private int generateOrders(int count, Map<String, String> customerNames) throws IOException {
        List<String> customerIds = new ArrayList<>(customerNames.keySet());
        try (AtomicCsvFile csv = newAtomicFile("orders.csv")) {
            BufferedWriter writer = csv.writer();
            writer.write("orderId,customerId,customerName,orderDate,totalAmount,status,eventId");
            writer.newLine();
            for (int i = 1; i <= count; i++) {
                String customerId = customerIds.get(randomInt(0, customerIds.size() - 1));
                String customerName = customerNames.get(customerId);
                String date = LocalDate.now().minusDays(randomInt(0, 29)).toString();
                String status = randomInt(1, 10) == 1 ? "FAILED" : "SUCCESS";
                writer.write(String.format(Locale.ROOT, "O-%05d,%s,%s,%s,%d,%s,EV-%03d",
                        i, customerId, CsvRowCodec.escape(customerName), date,
                        randomInt(100, 5_000) * 1_000, status, randomInt(1, 10)));
                writer.newLine();
            }
            csv.commit();
        }
        return count;
    }

    private int generateOrderDetails(int count) throws IOException {
        try (AtomicCsvFile csv = newAtomicFile("order_details.csv")) {
            BufferedWriter writer = csv.writer();
            writer.write("detailId,orderId,productId,quantity,unitPrice,subtotal");
            writer.newLine();
            for (int i = 1; i <= count; i++) {
                int quantity = randomInt(1, 2);
                int unitPrice = randomInt(50, 3_000) * 1_000;
                writer.write(String.format(Locale.ROOT, "D-%06d,O-%05d,P-%05d,%d,%d,%d",
                        i, i, randomInt(1, 5_000), quantity, unitPrice, quantity * unitPrice));
                writer.newLine();
            }
            csv.commit();
        }
        return count;
    }

    private int ensureTransactionsFile() throws IOException {
        Path path = dataDirectory.resolve("transactions.csv");
        if (!Files.exists(path)) {
            try (AtomicCsvFile csv = newAtomicFile("transactions.csv")) {
                csv.writer().write(TRANSACTION_HEADER);
                csv.writer().newLine();
                csv.commit();
            }
            return 0;
        }
        try (var lines = Files.lines(path, StandardCharsets.UTF_8)) {
            return (int) Math.max(0, lines.count() - 1);
        }
    }

    private Map<String, Customer> readExistingCustomers() throws IOException {
        Path path = dataDirectory.resolve("customers.csv");
        Map<String, Customer> customers = new LinkedHashMap<>();
        if (!Files.exists(path)) {
            return customers;
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            Customer customer = Customer.fromCsvLine(lines.get(i));
            if (customer != null && customer.getCustomerId() != null) {
                customers.put(customer.getCustomerId(), customer);
            }
        }
        return customers;
    }

    private AtomicCsvFile newAtomicFile(String fileName) throws IOException {
        return AtomicCsvFile.open(dataDirectory.resolve(fileName));
    }

    private static String randomTier() {
        int value = randomInt(1, 100);
        if (value > 95) return "DIAMOND";
        if (value > 80) return "GOLD";
        if (value > 60) return "SILVER";
        return "STANDARD";
    }

    private static String pick(String[] values) {
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    private static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
