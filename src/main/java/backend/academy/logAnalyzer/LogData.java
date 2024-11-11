package backend.academy.logAnalyzer;

public record LogData(String resource, String responseCode, long responseSize) {
}
