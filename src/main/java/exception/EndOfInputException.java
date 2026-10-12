package exception;

public class EndOfInputException extends RuntimeException {
    public EndOfInputException(String message) {
        super(message);
    }
}