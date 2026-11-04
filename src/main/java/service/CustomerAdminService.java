package service;

import model.Customer;
import model.enums.CustTier;
import repository.CustomerRepository;
import security.PasswordPolicy;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Các quy tắc nghiệp vụ cho tài khoản khách hàng do quản trị viên quản lý. */
public class CustomerAdminService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{9,11}$");
    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "BANNED");

    private final CustomerRepository customerRepository;
    private final Consumer<String> pendingOrderCanceller;

    public CustomerAdminService(CustomerRepository customerRepository,
                                Consumer<String> pendingOrderCanceller) {
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository");
        this.pendingOrderCanceller = Objects.requireNonNull(
                pendingOrderCanceller, "pendingOrderCanceller");
    }

    public List<Customer> list() {
        return customerRepository.getAll().stream()
                .sorted((first, second) -> first.getCustomerId().compareToIgnoreCase(
                        second.getCustomerId()))
                .toList();
    }

    public List<Customer> search(String keyword) {
        String normalized = required(keyword, "Search keyword").toLowerCase(Locale.ROOT);
        return customerRepository.getAll().stream()
                .filter(customer -> contains(customer.getCustomerId(), normalized)
                        || contains(customer.getEmail(), normalized)
                        || contains(customer.getName(), normalized)
                        || contains(customer.getPhone(), normalized))
                .toList();
    }

    public Customer create(String name, String email, String phone, String address,
                           String tier, String password) {
        String validName = validName(name);
        String validEmail = validEmail(email);
        String validPhone = validPhone(phone);
        String validAddress = required(address, "Address");
        CustTier validTier = parseTier(tier, CustTier.STANDARD);
        validatePassword(password);

        synchronized (customerRepository) {
            ensureEmailAvailable(validEmail, null);
            Customer customer = new Customer(
                    customerRepository.generateNewCustomerId(), validName, validEmail,
                    validPhone, validAddress, validTier, "ACTIVE");
            customer.setPassword(password);
            customerRepository.save(customer);
            return customer;
        }
    }

    public Customer update(String customerId, String name, String email, String phone,
                           String address, String tier, String newPassword) {
        Customer customer = requireCustomer(customerId);
        String updatedName = isBlank(name) ? customer.getName() : validName(name);
        String updatedEmail = isBlank(email) ? customer.getEmail() : validEmail(email);
        String updatedPhone = isBlank(phone) ? customer.getPhone() : validPhone(phone);
        String updatedAddress = isBlank(address) ? customer.getAddress() : address.trim();
        CustTier updatedTier = parseTier(tier, customer.getTier());

        if (!isBlank(newPassword)) {
            validatePassword(newPassword);
        }

        synchronized (customerRepository) {
            ensureEmailAvailable(updatedEmail, customer.getCustomerId());
            customer.setName(updatedName);
            customer.setEmail(updatedEmail);
            customer.setPhone(updatedPhone);
            customer.setAddress(updatedAddress);
            customer.setTier(updatedTier);
            if (!isBlank(newPassword)) {
                customer.setPassword(newPassword);
            }
            customerRepository.save(customer);
            return customer;
        }
    }

    public Customer setStatus(String customerId, String status) {
        String normalizedStatus = required(status, "Status").toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Status must be ACTIVE or BANNED.");
        }

        Customer customer = requireCustomer(customerId);
        if (normalizedStatus.equalsIgnoreCase(customer.getStatus().name())) {
            return customer;
        }
        if ("BANNED".equals(normalizedStatus)) {
            cancelPendingOrders(customerId, "ban");
        }
        customer.setStatus(normalizedStatus);
        customerRepository.save(customer);
        return customer;
    }

    public Customer delete(String customerId) {
        Customer customer = requireCustomer(customerId);
        cancelPendingOrders(customer.getCustomerId(), "delete");
        if (!customerRepository.deleteById(customer.getCustomerId())) {
            throw new IllegalStateException("Unable to delete customer account.");
        }
        return customer;
    }

    public Customer requireCustomer(String customerId) {
        String id = required(customerId, "Customer ID");
        Customer customer = customerRepository.findById(id).orElse(null);
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found: " + id);
        }
        return customer;
    }

    public void cancelPendingOrders(String customerId, String action) {
        try {
            pendingOrderCanceller.accept(customerId);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Unable to " + action + " the customer because not all PENDING/APPROVED orders could be cancelled.",
                    exception);
        }
    }

    public void ensureEmailAvailable(String email, String currentCustomerId) {
        boolean duplicate = customerRepository.getAll().stream()
                .anyMatch(customer -> customer.getEmail() != null
                        && customer.getEmail().equalsIgnoreCase(email)
                        && !customer.getCustomerId().equalsIgnoreCase(
                                currentCustomerId == null ? "" : currentCustomerId));
        if (duplicate) {
            throw new IllegalArgumentException("Email already exists.");
        }
    }

    public static void validatePassword(String password) {
        PasswordPolicy.validationError(password).ifPresent(error -> {
            throw new IllegalArgumentException(error);
        });
    }

    public static CustTier parseTier(String value, CustTier defaultTier) {
        if (isBlank(value)) {
            return defaultTier != null ? defaultTier : CustTier.STANDARD;
        }
        try {
            return CustTier.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Customer tier must be STANDARD, SILVER, GOLD, or DIAMOND.");
        }
    }

    public static String validName(String value) {
        String name = required(value, "Name");
        if (name.codePointCount(0, name.length()) < 2) {
            throw new IllegalArgumentException("Name must be at least 2 characters long.");
        }
        return name;
    }

    public static String validEmail(String value) {
        String email = required(value, "Email");
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        return email;
    }

    public static String validPhone(String value) {
        String phone = required(value, "Phone number");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Phone number must contain 9 to 11 digits.");
        }
        return phone;
    }

    public static String required(String value, String field) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    public static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
