package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import model.Customer;
import model.enums.CustomerStatus;
import repository.CustomerRepository;

public class AdminCustomerController {
    private final CustomerRepository customerRepository;

    public AdminCustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> viewUsers() {
        return customerRepository.getAll();
    }

    public Optional<Customer> findUser(String customerId) {
        return customerRepository.findById(customerId);
    }

    public Customer createUser(String name, String email, String phone, String address,
            String password) {
        requireText(name, "Name");
        requireText(email, "Email");
        requireText(phone, "Phone");
        if (!email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Email address is invalid.");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        }
        if (customerRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists.");
        }
        Customer customer = new Customer(customerRepository.generateNewCustomerId(),
                requireText(name, "Name"), requireText(email, "Email"),
                requireText(phone, "Phone"), address == null ? "" : address.trim(),
                "", model.enums.CustTier.STANDARD, CustomerStatus.ACTIVE.name(),
                security.PasswordSecurity.hash(requireText(password, "Password")), 0.0);
        customerRepository.save(customer);
        return customer;
    }

    public Customer updateUser(String customerId, String name, String phone, String address) {
        Customer customer = findRequired(customerId);
        customer.setName(requireText(name, "Name"));
        customer.setPhone(requireText(phone, "Phone"));
        customer.setAddress(address == null ? "" : address.trim());
        customerRepository.save(customer);
        return customer;
    }

    public boolean deleteUser(String customerId) {
        List<Customer> customers = new ArrayList<>(customerRepository.getAll());
        boolean removed = customers.removeIf(customer -> customer.getCustomerId().equals(customerId));
        if (removed) {
            customerRepository.saveAll(customers);
        }
        return removed;
    }

    public Customer updateStatus(String customerId, CustomerStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Customer status is required.");
        }
        Customer customer = findRequired(customerId);
        customer.setStatus(status);
        customerRepository.save(customer);
        return customer;
    }

    private Customer findRequired(String customerId) {
        return findUser(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer does not exist."));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty.");
        }
        return value.trim();
    }
}
