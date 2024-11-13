package backend.academy.logAnalyzer;

import lombok.Getter;

@Getter public enum ExceptionList {
    ERROR_WRITING_FILE("Error writing file");
    private final String exception;

    ExceptionList(String exception) {
        this.exception = exception;
    }
}
