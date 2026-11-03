package security;

/** Tên các biến môi trường được sử dụng bởi lớp xác thực. */
public final class SecurityEnvironment {
    public static final String ADMIN_USERNAME = "SHOPEE_ADMIN_USERNAME";
    public static final String LEGACY_ADMIN_USERNAME = "SHOPE_ADMIN_USERNAME";
    public static final String ADMIN_PASSWORD_HASH = "SHOPEE_ADMIN_PASSWORD_HASH";
    public static final String LEGACY_ADMIN_PASSWORD_HASH = "SHOPE_ADMIN_PASSWORD_HASH";

    private SecurityEnvironment() {
    }
}
