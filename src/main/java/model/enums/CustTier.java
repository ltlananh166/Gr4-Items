package model.enums;
public enum CustTier {
    STANDARD(0.0),
    SILVER(0.02),
    GOLD(0.05),
    DIAMOND(0.10);

    private final double discountRate;

    CustTier(double discountRate) {
        this.discountRate = discountRate;
    }

    public double getDiscountRate() {
        return discountRate;
    }

    public static CustTier fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return STANDARD;
        }
        String normalized = value.trim().toUpperCase();
        if ("PLANTIUM".equals(normalized)) {
            normalized = "DIAMOND";
        }
        return CustTier.valueOf(normalized);
    }
}