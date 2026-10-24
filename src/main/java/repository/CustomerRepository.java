package repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import model.Customer;
import security.PasswordSecurity;

public class CustomerRepository implements Repository<Customer> {
    private final Path filePath;

    public CustomerRepository(String filePath) {
        this.filePath = Paths.get(filePath);
    }

    @Override
    public List<Customer> getAll() {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        try {
            List<Customer> customers = new ArrayList<>();
            for (String line : Files.readAllLines(filePath, StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty() || line.trim().startsWith("customerId,")) {
                    continue;
                }
                Customer customer = Customer.fromCsvLine(line);
                if (customer != null) {
                    customers.add(customer);
                }
            }
            return customers;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read customers", e);
        }
    }

    @Override
    public void saveAll(List<Customer> customers) {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            List<String> lines = new ArrayList<>();
            lines.add("customerId,name,email,phone,address,avatarUrl,tier,status,password,walletBalance");
            for (Customer customer : customers) {
                if (customer == null) {
                    throw new IllegalArgumentException("Customer list cannot contain null values.");
                }
                if (!PasswordSecurity.isEncodedHash(customer.getPassword())) {
                    customer.setPassword(PasswordSecurity.hash(customer.getPassword()));
                }
                lines.add(customer.toCsvLine());
            }
            Files.write(filePath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save customers", e);
        }
    }

    public Optional<Customer> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        return getAll().stream()
                .filter(customer -> customer.getEmail() != null
                        && customer.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst();
    }

    public String generateNewCustomerId() {
        int maxId = 0;
        for (Customer customer : getAll()) {
            String id = customer.getCustomerId();
            if (id != null && id.matches("C-\\d+")) {
                try {
                    maxId = Math.max(maxId, Integer.parseInt(id.substring(2)));
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("Customer ID is outside the supported range: " + id, e);
                }
            }
        }
        if (maxId == Integer.MAX_VALUE) {
            throw new IllegalStateException("No customer IDs are available.");
        }
        return String.format("C-%05d", maxId + 1);
    }
}