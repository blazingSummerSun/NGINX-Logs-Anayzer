package backend.academy.logAnalyzer;

public record LogData(String ip, String user, String resource, String responseCode, long responseSize) {
}
