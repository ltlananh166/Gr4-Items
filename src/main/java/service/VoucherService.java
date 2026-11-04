package service;

import model.Voucher;
import model.enums.VoucherStatus;
import repository.VoucherRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Các quy tắc xác thực, tìm kiếm và CRUD cho phiếu giảm giá. */
public class VoucherService {
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]{3,30}$");
    private static final String PERCENTAGE = "PERCENTAGE";
    private static final String FIXED = "FIXED";

    private final VoucherRepository voucherRepository;

    public VoucherService(VoucherRepository voucherRepository) {
        this.voucherRepository = Objects.requireNonNull(voucherRepository, "voucherRepository");
    }

    public List<Voucher> list() {
        return voucherRepository.getAll().stream()
                .sorted((first, second) -> first.getCode().compareToIgnoreCase(second.getCode()))
                .toList();
    }

    public List<Voucher> listAvailable() {
        return list().stream()
                .filter(voucher -> voucher.getRemainingUses() > 0)
                .toList();
    }

    public List<Voucher> search(String keyword) {
        String normalized = required(keyword, "Search keyword").toLowerCase(Locale.ROOT);
        return voucherRepository.getAll().stream()
                .filter(voucher -> contains(voucher.getVoucherId(), normalized)
                        || contains(voucher.getCode(), normalized)
                        || contains(voucher.getDiscountType(), normalized))
                .toList();
    }

    public Voucher create(String code, String type, int value, int maxDiscount,
                          int minOrderAmount, int remainingUses) {
        VoucherData data = validate(code, type, value, maxDiscount,
                minOrderAmount, remainingUses);
        synchronized (voucherRepository) {
            ensureCodeAvailable(data.code(), null);
            Voucher voucher = new Voucher(voucherRepository.generateNewVoucherId(),
                    data.code(), data.type(), BigDecimal.valueOf(data.value()),
                    BigDecimal.valueOf(data.minOrderAmount()),
                    BigDecimal.valueOf(data.maxDiscount()), null, null, VoucherStatus.ACTIVE);
            voucher.setRemainingUses(data.remainingUses());
            voucherRepository.save(voucher);
            return voucher;
        }
    }

    public Voucher update(String idOrCode, String code, String type, Integer value,
                          Integer maxDiscount, Integer minOrderAmount, Integer remainingUses) {
        Voucher voucher = requireVoucher(idOrCode);
        String updatedCode = isBlank(code) ? voucher.getCode() : code;
        String updatedType = isBlank(type) ? voucher.getDiscountType() : type;
        int updatedValue = value == null ? voucher.getDiscountAmount().intValueExact() : value;
        int updatedMax = maxDiscount == null
                ? voucher.getMaxDiscountValue().intValueExact() : maxDiscount;
        int updatedMin = minOrderAmount == null
                ? voucher.getMinOrderValue().intValueExact() : minOrderAmount;
        int updatedUses = remainingUses == null ? voucher.getRemainingUses() : remainingUses;
        VoucherData data = validate(updatedCode, updatedType, updatedValue,
                updatedMax, updatedMin, updatedUses);

        synchronized (voucherRepository) {
            ensureCodeAvailable(data.code(), voucher.getVoucherId());
            voucher.setCode(data.code());
            voucher.setDiscountType(data.type());
            voucher.setDiscountAmount(BigDecimal.valueOf(data.value()));
            voucher.setMaxDiscountValue(BigDecimal.valueOf(data.maxDiscount()));
            voucher.setMinOrderValue(BigDecimal.valueOf(data.minOrderAmount()));
            voucher.setRemainingUses(data.remainingUses());
            voucherRepository.save(voucher);
            return voucher;
        }
    }

    public Voucher delete(String idOrCode) {
        Voucher voucher = requireVoucher(idOrCode);
        if (!voucherRepository.deleteById(voucher.getVoucherId())) {
            throw new IllegalStateException("Unable to delete voucher.");
        }
        return voucher;
    }

    public Voucher requireVoucher(String idOrCode) {
        String key = required(idOrCode, "Voucher code or ID");
        Voucher voucher = voucherRepository.findById(key).orElse(null);
        if (voucher == null) {
            voucher = voucherRepository.findByCode(key).orElse(null);
        }
        if (voucher == null) {
            throw new IllegalArgumentException("Voucher not found: " + key);
        }
        return voucher;
    }

    public void ensureCodeAvailable(String code, String currentVoucherId) {
        boolean duplicate = voucherRepository.getAll().stream()
                .anyMatch(voucher -> voucher.getCode() != null
                        && voucher.getCode().equalsIgnoreCase(code)
                        && !voucher.getVoucherId().equalsIgnoreCase(
                                currentVoucherId == null ? "" : currentVoucherId));
        if (duplicate) {
            throw new IllegalArgumentException("Voucher code already exists.");
        }
    }

    public static VoucherData validate(String code, String type, int value,
                                        int maxDiscount, int minOrderAmount,
                                        int remainingUses) {
        String validCode = required(code, "Voucher code").toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(validCode).matches()) {
            throw new IllegalArgumentException(
                    "Voucher code must contain 3 to 30 letters, digits, hyphens, or underscores.");
        }
        String validType = required(type, "Voucher type").toUpperCase(Locale.ROOT);
        if (!PERCENTAGE.equals(validType) && !FIXED.equals(validType)) {
            throw new IllegalArgumentException("Voucher type must be PERCENTAGE or FIXED.");
        }
        if (value <= 0 || PERCENTAGE.equals(validType) && value > 100) {
            throw new IllegalArgumentException(
                    "Discount must be greater than zero; percentage discount cannot exceed 100.");
        }
        if (minOrderAmount < 0 || remainingUses < 0) {
            throw new IllegalArgumentException(
                    "Minimum order value and remaining uses cannot be negative.");
        }
        int normalizedMax = FIXED.equals(validType) ? value : maxDiscount;
        if (PERCENTAGE.equals(validType) && normalizedMax <= 0) {
            throw new IllegalArgumentException("Maximum discount must be greater than zero.");
        }
        return new VoucherData(validCode, validType, value, normalizedMax,
                minOrderAmount, remainingUses);
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

    private record VoucherData(String code, String type, int value, int maxDiscount,
                               int minOrderAmount, int remainingUses) {
    }
}
