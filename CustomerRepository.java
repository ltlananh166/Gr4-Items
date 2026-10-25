package repository;

import model.Customer;
import security.PasswordSecurity;

import java.util.List;
import java.util.Optional;

public class CustomerRepository extends CsvRepository<Customer> {
    
    public CustomerRepository() {
        super(Customer.class, "customers.csv", "customerId,name,email,phone,address,avatarUrl,tier,status,password,walletBalance");
    }

    public CustomerRepository(String dataDirectory) {
        super(Customer.class, dataDirectory, "customers.csv", "customerId,name,email,phone,address,avatarUrl,tier,status,password,walletBalance");
    }

    public String generateNewCustomerId() {
        int maxId = findAll().stream()
                .map(Customer::getCustomerId)
                .filter(id -> id != null && id.startsWith("C-"))
                .mapToInt(id -> {
                    try {
                        return Integer.parseInt(id.substring(2));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return String.format("C-%05d", maxId + 1);
    }

    public Optional<Customer> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        String normalizedEmail = email.trim();
        return findAll().stream()
                .filter(customer -> customer.getEmail() != null
                        && customer.getEmail().equalsIgnoreCase(normalizedEmail))
                .findFirst();
    }

    @Override
public void save(Customer customer) {
    securePasswordForStorage(customer);
    super.saveAll(java.util.Collections.singletonList(customer));
}

    @Override
    public void saveAll(List<Customer> customers) {
        customers.forEach(this::securePasswordForStorage);
        super.saveAll(customers);
    }

    private void securePasswordForStorage(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer must not be null");
        }
        String storedPassword = customer.getPassword();
        if (storedPassword != null && !storedPassword.isEmpty()
                && !PasswordSecurity.isEncodedHash(storedPassword)) {
            customer.setPassword(PasswordSecurity.hash(storedPassword));
        }
    }
}