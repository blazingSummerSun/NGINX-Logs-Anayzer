package backend.academy.logAnalyzer.logs;

/**
 * Enumeration representing the parameters in a log entry.
 */
public enum LogParams {
    /**
     * The IP address from which the request originated.
     */
    REMOTE_ADDR,

    /**
     * The user who made the request.
     */
    REMOTE_USER,

    /**
     * The timestamp of the request.
     */
    TIMESTAMP,

    /**
     * The request made by the user.
     */
    REQUEST,

    /**
     * The HTTP status code returned by the server.
     */
    STATUS,

    /**
     * The size of the response body in bytes.
     */
    BODY_BYTES_SENT,

    /**
     * The HTTP referer header.
     */
    HTTP_REFERER,

    /**
     * The HTTP user agent header.
     */
    HTTP_USER_AGENT
}
