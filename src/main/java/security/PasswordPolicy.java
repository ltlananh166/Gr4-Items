package security;

import java.util.Optional;

/** Các quy tắc xác thực mật khẩu khách hàng mới tạo. */
public final class PasswordPolicy {
    public static final int MIN_LENGTH = 6;
    public static final int MAX_LENGTH = 256;

    private PasswordPolicy() {
    }

    public static Optional<String> validationError(String password) {
        if (password == null || password.isBlank()) {
            return Optional.of("Password is required.");
        }

        int length = password.codePointCount(0, password.length());
        if (length < MIN_LENGTH) {
            return Optional.of("Password must be at least " + MIN_LENGTH + " characters long.");
        }
        if (length > MAX_LENGTH) {
            return Optional.of("Password must not exceed " + MAX_LENGTH + " characters.");
        }
        return Optional.empty();
    }
}
