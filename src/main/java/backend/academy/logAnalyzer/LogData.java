package backend.academy.logAnalyzer;

/**
 * Represents an information about a single log-string.
 *
 * @param ip           the IP address from which the request originated.
 * @param user         the user who made the request.
 * @param resource     the resource that was accessed.
 * @param responseCode the HTTP response code returned.
 * @param responseSize the size of the response in bytes.
 */
public record LogData(String ip, String user, String resource, String responseCode, long responseSize) {
}
