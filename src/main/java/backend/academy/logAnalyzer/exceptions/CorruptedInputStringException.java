package backend.academy.logAnalyzer.exceptions;

public class CorruptedInputStringException extends RuntimeException {
    public CorruptedInputStringException(String message) {
        super(message);
    }
}
