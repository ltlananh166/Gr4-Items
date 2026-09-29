package model;

import model.enums.CustTier;

public class Voucher extends BaseEntity {

    private String voucherId;
    private String code;
    private double discountAmount;
    private double minOrderValue;
    private CustTier requiredTier;
    private boolean isUsed;

    public Voucher() {
        this.requiredTier = CustTier.STANDARD;
        this.isUsed = false;
    }

    public Voucher(String voucherId, String code, double discountAmount, double minOrderValue, CustTier requiredTier, boolean isUsed) {
        this.voucherId = voucherId;
        this.code = code;
        this.discountAmount = discountAmount;
        this.minOrderValue = minOrderValue;
        this.requiredTier = (requiredTier != null) ? requiredTier : CustTier.STANDARD;
        this.isUsed = isUsed;
    }

    @Override
    public String getId() {
        return voucherId;
    }

    @Override
    public String toCsvLine() {
        return String.format("%s,%s,%.2f,%.2f,%s,%b",
                voucherId, code, discountAmount, minOrderValue, requiredTier.name(), isUsed);
    }

    public static Voucher fromCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        String[] parts = line.split(",");
        if (parts.length < 6) {
            return null;
        }
        return new Voucher(
                parts[0].trim(),
                parts[1].trim(),
                Double.parseDouble(parts[2].trim()),
                Double.parseDouble(parts[3].trim()),
                CustTier.valueOf(parts[4].trim().toUpperCase()),
                Boolean.parseBoolean(parts[5].trim())
        );
    }

    public String getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(String voucherId) {
        this.voucherId = voucherId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(double minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public CustTier getRequiredTier() {
        return requiredTier;
    }

    public void setRequiredTier(CustTier requiredTier) {
        this.requiredTier = requiredTier;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }
}



