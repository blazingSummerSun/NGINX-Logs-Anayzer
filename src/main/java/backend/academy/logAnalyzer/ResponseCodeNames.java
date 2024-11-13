package backend.academy.logAnalyzer;

import lombok.Getter;

@Getter public enum ResponseCodeNames {
    SERVER_ERROR_RESPONSES("500"), CLIENT_ERROR_RESPONSES("400"), REDIRECT_RESPONSES("300"), SUCCESS_RESPONSES("200"),
    INFORMATIONAL_RESPONSES("100");
    private final String responseCode;

    ResponseCodeNames(String responseCode) {
        this.responseCode = responseCode;
    }
}
