package exception;

public class PurchaseLimitExceededException extends RuntimeException {
    public PurchaseLimitExceededException(String message) {
        super(message);
    }
}