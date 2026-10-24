package model;

import java.util.ArrayList;
import java.util.List;
import model.enums.CustTier;
import model.enums.CustomerStatus;

public class Customer extends BaseEntity {

    private String customerId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String avatarUrl;
    private CustTier tier;
    private CustomerStatus status;
    private String password;
    private double walletBalance;

    public Customer() {
        this.tier = CustTier.STANDARD;
        this.status = CustomerStatus.ACTIVE;
    }

    public Customer(String customerId, String name, String email, String phone,
            String address, String avatarUrl) {
        this(customerId, name, email, phone, address, avatarUrl, CustTier.STANDARD,
                "ACTIVE", "", 0.0);
    }

    public Customer(String customerId, String name, String email, String phone,
            String address, String avatarUrl, CustTier tier, String status) {
        this(customerId, name, email, phone, address, avatarUrl, tier, status, "", 0.0);
    }

    public Customer(String customerId, String name, String email, String phone,
            String address, String avatarUrl, CustTier tier, String status,
            String password, double walletBalance) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.tier = tier == null ? CustTier.STANDARD : tier;
        this.status = CustomerStatus.fromValue(status);
        this.password = password == null ? "" : password;
        setWalletBalance(walletBalance);
    }

    @Override
    public String getId() {
        return customerId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",",
                csv(customerId),
                csv(name),
                csv(email),
                csv(phone),
                csv(address),
                csv(avatarUrl),
                csv(tier.name()),
                csv(status.name()),
                csv(password),
                Double.toString(walletBalance));
    }

    public static Customer fromCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        List<String> columns = parseCsvLine(line);
        if (columns.size() == 7) {
            return fromLegacyCsv(columns);
        }
        if (columns.size() != 10) {
            throw new IllegalArgumentException("Customer CSV row must contain 10 columns.");
        }
        try {
            return new Customer(
                    columns.get(0),
                    columns.get(1),
                    columns.get(2),
                    columns.get(3),
                    columns.get(4),
                    columns.get(5),
                    CustTier.fromValue(columns.get(6)),
                    columns.get(7),
                    columns.get(8),
                    Double.parseDouble(columns.get(9)));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid customer CSV row: " + line, e);
        }
    }

    private static Customer fromLegacyCsv(List<String> columns) {
        try {
            String email = columns.get(4).isEmpty() ? columns.get(1) : columns.get(4);
            return new Customer(
                    columns.get(0),
                    columns.get(3),
                    email,
                    "",
                    "",
                    "",
                    CustTier.fromValue(columns.get(5)),
                    "ACTIVE",
                    columns.get(2),
                    Double.parseDouble(columns.get(6)));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid legacy customer CSV row.", e);
        }
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    static List<String> parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                columns.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unclosed quoted value in customer CSV row.");
        }
        columns.add(value.toString());
        return columns;
    }

    public String getCustomerId() {
        return customerId;
    }

    @Override
    public void setId(String id) {
        this.customerId = id;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public CustTier getTier() {
        return tier;
    }

    public void setTier(CustTier tier) {
        this.tier = tier == null ? CustTier.STANDARD : tier;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = CustomerStatus.fromValue(status);
    }

    public void setStatus(CustomerStatus status) {
        this.status = status == null ? CustomerStatus.ACTIVE : status;
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    public void ban() {
        status = CustomerStatus.BANNED;
    }

    public void unban() {
        status = CustomerStatus.ACTIVE;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? "" : password;
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(double walletBalance) {
        if (Double.isNaN(walletBalance) || Double.isInfinite(walletBalance) || walletBalance < 0) {
            throw new IllegalArgumentException("Wallet balance must be a finite non-negative value.");
        }
        this.walletBalance = walletBalance;
    }
}
