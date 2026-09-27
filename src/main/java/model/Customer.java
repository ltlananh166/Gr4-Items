package model;

import model.enums.CustTier;

public class Customer extends BaseEntity {

    private String customerId;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private CustTier tier;
    private double walletBalance;

    public Customer() {
        this.tier = CustTier.STANDARD;
        this.walletBalance = 0.0;
    }

    public Customer(String customerId, String username, String password, String fullName, String email, CustTier tier, double walletBalance) {
        this.customerId = customerId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.tier = (tier != null) ? tier : CustTier.STANDARD;
        this.walletBalance = walletBalance;
    }

    @Override
    public String getId() {
        return customerId;
    }

    @Override
    public String toCsvLine() {
        return String.format("%s,%s,%s,%s,%s,%s,%.2f",
                customerId, username, password, fullName, email, tier.name(), walletBalance);
    }

    public static Customer fromCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        String[] parts = line.split(",");
        if (parts.length < 7) {
            return null;
        }
        return new Customer(
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                CustTier.valueOf(parts[5].trim().toUpperCase()),
                Double.parseDouble(parts[6].trim())
        );
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CustTier getTier() {
        return tier;
    }

    public void setTier(CustTier tier) {
        this.tier = tier;
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(double walletBalance) {
        this.walletBalance = walletBalance;
    }
}


