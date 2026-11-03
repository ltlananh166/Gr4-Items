package security;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Thông tin xác thực quản trị viên bất biến, được tải từ các biến môi trường của tiến trình. */
public final class AdminCredentials {
    private final String username;
    private final String passwordHash;

    private AdminCredentials(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public static Optional<AdminCredentials> fromEnvironment() {
        return from(System.getenv());
    }

    public static Optional<AdminCredentials> from(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");
        String username = firstAvailable(environment,
                SecurityEnvironment.ADMIN_USERNAME,
                SecurityEnvironment.LEGACY_ADMIN_USERNAME);
        String passwordHash = firstAvailable(environment,
                SecurityEnvironment.ADMIN_PASSWORD_HASH,
                SecurityEnvironment.LEGACY_ADMIN_PASSWORD_HASH);

        if (username == null && passwordHash == null) {
            return Optional.empty();
        }
        if (username == null || passwordHash == null) {
            throw new IllegalStateException(
                    SecurityEnvironment.ADMIN_USERNAME + " and "
                            + SecurityEnvironment.ADMIN_PASSWORD_HASH
                            + " must be configured together; legacy aliases "
                            + SecurityEnvironment.LEGACY_ADMIN_USERNAME + " and "
                            + SecurityEnvironment.LEGACY_ADMIN_PASSWORD_HASH + " are also accepted");
        }
        if (!PasswordSecurity.isEncodedHash(passwordHash)) {
            throw new IllegalStateException(
                    SecurityEnvironment.ADMIN_PASSWORD_HASH + " must contain an Argon2id PHC hash");
        }
        return Optional.of(new AdminCredentials(username, passwordHash));
    }

    private static String firstAvailable(Map<String, String> environment, String... names) {
        for (String name : names) {
            String value = normalized(environment.get(name));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public boolean authenticate(String suppliedUsername, String suppliedPassword) {
        boolean passwordMatches = PasswordSecurity.matches(suppliedPassword, passwordHash);
        return PasswordSecurity.constantTimeEquals(username, suppliedUsername) && passwordMatches;
    }

    public String username() {
        return username;
    }

    private static String normalized(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
