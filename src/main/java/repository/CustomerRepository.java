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
                if (line.trim().isEmpty() || line.startsWith("customerId,")) {
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
            lines.add("customerId,username,password,fullName,email,tier,walletBalance");
            for (Customer customer : customers) {
                lines.add(customer.toCsvLine());
            }
            Files.write(filePath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save customers", e);
        }
    }

    public Optional<Customer> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return getAll().stream()
                .filter(customer -> customer.getUsername() != null
                        && customer.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst();
    }
}