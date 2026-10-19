package model.enums;

public enum CustomerStatus {
    ACTIVE,
    BANNED;

    public static CustomerStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ACTIVE;
        }
        return CustomerStatus.valueOf(value.trim().toUpperCase());
    }
}
