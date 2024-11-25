package backend.academy.logAnalyzer.exceptions;

public class EmptyInputStringException extends RuntimeException {
    public EmptyInputStringException(String message) {
        super(message);
    }
}
