package backend.academy.logAnalyzer;

import lombok.Getter;

/**
 * Enumeration representing different categories of HTTP response codes.
 */
@Getter public enum ResponseCodeNames {
    /**
     * Represents server error responses (5xx).
     */
    SERVER_ERROR_RESPONSES("500"),

    /**
     * Represents client error responses (4xx).
     */
    CLIENT_ERROR_RESPONSES("400"),

    /**
     * Represents redirection responses (3xx).
     */
    REDIRECT_RESPONSES("300"),

    /**
     * Represents successful responses (2xx).
     */
    SUCCESS_RESPONSES("200"),

    /**
     * Represents informational responses (1xx).
     */
    INFORMATIONAL_RESPONSES("100");

    private final String responseCode;

    /**
     * Constructs a ResponseCodeNames enum with the specified response code.
     *
     * @param responseCode the HTTP response code category.
     */
    ResponseCodeNames(String responseCode) {
        this.responseCode = responseCode;
    }
}
