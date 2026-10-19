package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import model.enums.CustTier;
import model.enums.VoucherStatus;

public class Voucher extends BaseEntity {
    private String voucherId;
    private String code;
    private String discountType = "FIXED";
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal minOrderValue = BigDecimal.ZERO;
    private BigDecimal maxDiscountValue = BigDecimal.ZERO;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private VoucherStatus status = VoucherStatus.ACTIVE;

    public Voucher() { }

    public Voucher(String voucherId, String code, double discountAmount,
            double minOrderValue, CustTier requiredTier, boolean isUsed) {
        this(voucherId, code, "FIXED", BigDecimal.valueOf(discountAmount),
                BigDecimal.valueOf(minOrderValue), BigDecimal.valueOf(discountAmount),
                null, null, isUsed ? VoucherStatus.USED : VoucherStatus.ACTIVE);
    }

    public Voucher(String voucherId, String code, String discountType,
            BigDecimal discountAmount, BigDecimal minOrderValue, BigDecimal maxDiscountValue,
            LocalDateTime validFrom, LocalDateTime validTo, VoucherStatus status) {
        this.voucherId = voucherId;
        this.code = code;
        setDiscountType(discountType);
        setDiscountAmount(discountAmount);
        setMinOrderValue(minOrderValue);
        setMaxDiscountValue(maxDiscountValue);
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.status = status == null ? VoucherStatus.ACTIVE : status;
        validateValidityRange();
    }

    public Voucher(String voucherId, String code, String discountType,
            BigDecimal discountAmount, BigDecimal minOrderValue, BigDecimal maxDiscountValue,
            LocalDateTime validFrom, LocalDateTime validTo, CustTier ignoredRequiredTier,
            VoucherStatus status) {
        this(voucherId, code, discountType, discountAmount, minOrderValue, maxDiscountValue,
                validFrom, validTo, status);
    }

    @Override
    public String getId() { return voucherId; }

    @Override
    public String toCsvLine() {
        return String.join(",", value(voucherId), value(code), discountType,
                discountAmount.toPlainString(), minOrderValue.toPlainString(),
                maxDiscountValue.toPlainString(), value(validFrom), value(validTo), status.name());
    }

    public static Voucher fromCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        List<String> columns = Customer.parseCsvLine(line);
        try {
            if (columns.size() == 6) {
                return new Voucher(columns.get(0).trim(), columns.get(1).trim(),
                        Double.parseDouble(columns.get(2).trim()),
                        Double.parseDouble(columns.get(3).trim()),
                        CustTier.fromValue(columns.get(4)), Boolean.parseBoolean(columns.get(5).trim()));
            }
            if (columns.size() == 10) {
                return new Voucher(columns.get(0), columns.get(1), columns.get(2),
                        new BigDecimal(columns.get(3)), new BigDecimal(columns.get(4)),
                        new BigDecimal(columns.get(5)), parseDate(columns.get(6)), parseDate(columns.get(7)),
                        CustTier.fromValue(columns.get(8)), VoucherStatus.valueOf(columns.get(9).toUpperCase()));
            }
            if (columns.size() != 9) {
                throw new IllegalArgumentException("Voucher CSV row must contain 9 columns.");
            }
            return new Voucher(columns.get(0), columns.get(1), columns.get(2),
                    new BigDecimal(columns.get(3)), new BigDecimal(columns.get(4)),
                    new BigDecimal(columns.get(5)), parseDate(columns.get(6)), parseDate(columns.get(7)),
                    VoucherStatus.valueOf(columns.get(8).toUpperCase()));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid voucher CSV row: " + line, e);
        }
    }

    public boolean isValid() {
        if (status != VoucherStatus.ACTIVE) return false;
        LocalDateTime now = LocalDateTime.now();
        return (validFrom == null || !now.isBefore(validFrom))
                && (validTo == null || now.isBefore(validTo));
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        Objects.requireNonNull(orderAmount, "Order amount cannot be null.");
        if (orderAmount.signum() < 0) throw new IllegalArgumentException("Order amount cannot be negative.");
        BigDecimal discount;
        if ("PERCENTAGE".equalsIgnoreCase(discountType)
                || "PERCENT".equalsIgnoreCase(discountType)) {
            discount = orderAmount.multiply(discountAmount).movePointLeft(2);
        } else {
            discount = discountAmount;
        }
        if (maxDiscountValue.signum() > 0) discount = discount.min(maxDiscountValue);
        return discount.min(orderAmount).max(BigDecimal.ZERO);
    }

    private void validateValidityRange() {
        if (validFrom != null && validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("Voucher expiry must be after its start time.");
        }
    }

    private static LocalDateTime parseDate(String value) {
        return value == null || value.trim().isEmpty() ? null : LocalDateTime.parse(value);
    }

    private static String value(Object value) { return value == null ? "" : value.toString(); }

    public String getVoucherId() { return voucherId; }
    @Override
    public void setId(String id) { this.voucherId = id; }
    public void setVoucherId(String voucherId) { this.voucherId = voucherId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) {
        this.discountType = discountType == null || discountType.trim().isEmpty()
                ? "FIXED" : discountType.trim().toUpperCase();
        if (!"FIXED".equals(this.discountType) && !"PERCENT".equals(this.discountType)
                && !"PERCENTAGE".equals(this.discountType)) {
            throw new IllegalArgumentException("Discount type must be FIXED or PERCENTAGE.");
        }
    }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { setDiscountAmount(BigDecimal.valueOf(discountAmount)); }
    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = nonNegative(discountAmount, "Discount amount");
    }
    public BigDecimal getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(double minOrderValue) { setMinOrderValue(BigDecimal.valueOf(minOrderValue)); }
    public void setMinOrderValue(BigDecimal minOrderValue) {
        this.minOrderValue = nonNegative(minOrderValue, "Minimum order value");
    }
    public BigDecimal getMaxDiscountValue() { return maxDiscountValue; }
    public void setMaxDiscountValue(BigDecimal maxDiscountValue) {
        this.maxDiscountValue = nonNegative(maxDiscountValue, "Maximum discount value");
    }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; validateValidityRange(); }
    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; validateValidityRange(); }
    public VoucherStatus getStatus() { return status; }
    public void setStatus(VoucherStatus status) { this.status = status == null ? VoucherStatus.ACTIVE : status; }
    public boolean isUsed() { return status == VoucherStatus.USED; }
    public void setUsed(boolean used) { status = used ? VoucherStatus.USED : VoucherStatus.ACTIVE; }

    private static BigDecimal nonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " cannot be null.");
        if (value.signum() < 0) throw new IllegalArgumentException(field + " cannot be negative.");
        return value;
    }
}
