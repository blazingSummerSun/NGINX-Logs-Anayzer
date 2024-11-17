package backend.academy.logAnalyzer;

import lombok.Getter;

/**
 * Enumeration representing different exception messages used in the log analyzer.
 * Each enum element stores a specific exception message string.
 */
@Getter public enum ExceptionList {
    /**
     * Represents an error message for writing files.
     */
    ERROR_WRITING_FILE("Error writing file!"),

    /**
     * Represents an error message for invalid path or pattern.
     */
    INVALID_PATH_PATTERN("Invalid path or pattern!"),

    /**
     * Represents an error message for reading input strings.
     */
    ERROR_INPUT_STRING("Impossible to read the string!"),

    /**
     * Represents an error message for fetching URLs.
     */
    ERROR_FETCHING_URL("Error fetching URL!"),

    /**
     * Represents an error message for invalid filter fields.
     */
    INVALID_FILTER_FIELD("Invalid filter field!");
    /**
     * The exception message associated with the enum constant.
     */
    private final String exception;

    /**
     * Constructor to initialize the exception message.
     *
     * @param exception the exception message string.
     */
    ExceptionList(String exception) {
        this.exception = exception;
    }
}
