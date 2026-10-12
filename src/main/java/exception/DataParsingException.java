package exception;

public class DataParsingException extends RuntimeException {
    public DataParsingException(String message) {
        super(message);
    }

    public DataParsingException(String mesage, Throwable cause) {
        super(message, cause);
    }
}