package backend.academy.logAnalyzer;

import lombok.Getter;

@Getter public enum ExceptionList {
    ERROR_WRITING_FILE("Error writing file!"), INVALID_PATH_PATTERN("Invalid path or pattern!"),
    ERROR_INPUT_STRING("Impossible to read the string!"), ERROR_FETCHING_URL("Error fetching URL!"),;
    private final String exception;

    ExceptionList(String exception) {
        this.exception = exception;
    }
}
