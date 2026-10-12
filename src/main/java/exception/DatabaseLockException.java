package exception;

public class DatabaseLockException extends RuntimeException {
    public DatabaseLockException(String message) {
        super(message);
    }

    public DatabaseLockException(String message, Throwable cause) {
        super(message, cause);
    }
}