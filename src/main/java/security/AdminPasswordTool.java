package security;

import java.io.Console;
import java.util.Arrays;
import java.util.Scanner;

/** Công cụ tương tác in mã băm Argon2id mà không nhận mật khẩu làm đối số. */
public final class AdminPasswordTool {
    private static final int MIN_ADMIN_PASSWORD_LENGTH = 12;

    private AdminPasswordTool() {
    }

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        char[] password = readPassword();
        try {
            if (password.length < MIN_ADMIN_PASSWORD_LENGTH) {
                throw new IllegalArgumentException(
                        "Admin password must be at least 12 characters long.");
            }
            System.out.println(PasswordSecurity.hash(new String(password)));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static char[] readPassword() {
        Console console = System.console();
        if (console != null) {
            char[] value = console.readPassword("Enter the admin password to hash: ");
            if (value == null) {
                throw new IllegalStateException("Admin password generation cancelled.");
            }
            return value;
        }

        System.err.println("Warning: the current terminal does not support masked input.");
        System.out.print("Enter the admin password to hash: ");
        Scanner scanner = new Scanner(System.in);
        if (!scanner.hasNextLine()) {
            throw new IllegalStateException("Unable to read the admin password.");
        }
        return scanner.nextLine().toCharArray();
    }
}
