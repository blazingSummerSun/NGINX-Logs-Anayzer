package backend.academy.logAnalyzer.logs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * The class is responsible for analyzing log files and generating statistical report
 * based on given data. It supports both local log files and logs from remote URLs,
 * filtering by date range (optional parameters) and user agent (optional parameter),
 * and generating output in one of two formats (.md/.adoc).
 *
 * <p>This class processes log entries, collects relevant metrics (total requests, average response size,
 * resource access frequency, response code frequency, 95p answer size, the most frequent username, and
 * the most frequent IP address), and generates reports containing metrics described above.
 *
 * <p>Features:
 * <ul>
 *     <li>Supports filtering by date range and http_user_agent fields</li>
 *     <li>Processes local log files matching a glob pattern or remote logs from a URL</li>
 *     <li>Handles multiple log files and generates the output report based on the provided format</li>
 * </ul>
 */

@Getter @Slf4j public class LogAnalyzer {
    /**
     * The percentage (95%) used for calculating the response size percentile.
     */
    private static final double PERCENTILE = 0.95;

    /**
     * Regular expression pattern for parsing log entries.
     */
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "^(\\S+) - (\\S+) \\[([^]]+)] "
            + "\"([^\"]+)\" (\\d{3}) (\\d+) "
            + "\"([^\"]*)\" \"([^\"]*)\"$"
    );

    /**
     * Formatter for parsing date and time in log entries.
     */
    private static final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");

    /**
     * The output stream for writing logs and reports.
     */

    private final List<String> processedFiles = new ArrayList<>();

    /**
     * Analyzes log files or a log URL based on the given filters and generates a report.
     *
     * @param path        the file path or URL to the logs
     * @param fromDate    the start date-time for filtering logs
     * @param toDate      the end date-time for filtering logs
     * @param agentFilter the filter for matching specific user agents
     */
    public CollectedData analyze(
        String path,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        String agentFilter
    ) {
        AtomicLong totalRequests = new AtomicLong();
        Map<String, AtomicLong> ips = new ConcurrentHashMap<>();
        Map<String, AtomicLong> users = new ConcurrentHashMap<>();
        Map<String, AtomicLong> resourceFrequency = new ConcurrentHashMap<>();
        Map<String, AtomicLong> responseCodeFrequency = new ConcurrentHashMap<>();
        AtomicLong totalResponseSize = new AtomicLong();
        List<Long> responseSizes = new ArrayList<>();

        Supplier<Stream<LogData>> logDataStreamSupplier = () -> getLogDataStream(path, fromDate, toDate, agentFilter);

        try (Stream<LogData> logDataStream = logDataStreamSupplier.get()) {
            logDataStream.forEach(log -> {
                totalRequests.incrementAndGet();
                resourceFrequency.computeIfAbsent(log.resource(), k -> new AtomicLong()).incrementAndGet();
                responseCodeFrequency.computeIfAbsent(log.responseCode(), k -> new AtomicLong()).incrementAndGet();
                ips.computeIfAbsent(log.ip(), k -> new AtomicLong()).incrementAndGet();
                users.computeIfAbsent(log.user(), k -> new AtomicLong()).incrementAndGet();
                totalResponseSize.addAndGet(log.responseSize());
                responseSizes.add(log.responseSize());
            });
        }
        double percentile = calculatePercentile(responseSizes);
        return new CollectedData(totalRequests.get(), resourceFrequency,
            responseCodeFrequency, totalResponseSize.get(), responseSizes, ips, users, percentile);
    }

    /**
     * Creates a stream of {@code LogData} objects based on the given path.
     * If the path is a valid URL, it fetches log data from the URL.
     * Otherwise, it fetches log data from files matching the path pattern.
     *
     * @param path        the file path or URL to the logs
     * @param fromDate    the start date-time for filtering log entries
     * @param toDate      the end date-time for filtering log entries
     * @param agentFilter the filter for matching specific user agents
     * @return a stream of {@code LogData} objects parsed from the specified path
     */
    private Stream<LogData> getLogDataStream(
        String path,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        String agentFilter
    ) {
        if (isValidURL(path)) {
            processedFiles.add(path);
            return createStreamFromURL(path, fromDate, toDate, agentFilter);
        } else {
            List<Path> logFiles = getMatchingFiles(path);
            logFiles.forEach(file -> processedFiles.add(file.toString()));
            return logFiles.stream().flatMap(file -> parseFileToStream(file, fromDate, toDate, agentFilter));
        }
    }

    /**
     * Finds log files (in {@code /src/resources/logs}) folder matching the given glob pattern and returns
     * a list of their paths.
     *
     * @param userPathPattern the glob pattern for matching log files
     * @return a list of paths to the matching log files
     */
    private static List<Path> getMatchingFiles(String userPathPattern) {
        List<Path> logFiles = new ArrayList<>();
        String mainFolder = userPathPattern.split("/")[0];
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + userPathPattern);

        try {
            Files.walkFileTree(Paths.get(mainFolder), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (pathMatcher.matches(file)) {
                        logFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (InvalidPathException | NoSuchFileException e) {
            log.error("Impossible to reach some files", e);
        } catch (IOException e) {
            log.error("Error during files searching has been occurred!", e);
        }
        return logFiles;
    }

    /**
     * Calculates the specified percentile of response sizes.
     *
     * @param responseSizes a list of response sizes
     * @return the value at the specified percentile or {@code 0} if list is empty
     */
    private double calculatePercentile(List<Long> responseSizes) {
        if (responseSizes.isEmpty()) {
            return 0;
        }

        Collections.sort(responseSizes);

        int index = (int) Math.ceil(PERCENTILE * responseSizes.size()) - 1;
        return responseSizes.get(index);
    }

    /**
     * Parses a log file into a stream of {@code LogData} objects based on the given filters.
     *
     * @param filePath    the path to the log file
     * @param fromDate    the start date-time for filtering logs
     * @param toDate      the end date-time for filtering logs
     * @param agentFilter the filter for matching specific user agents
     * @return a stream of {@code LogData} objects parsed from the log file
     */
    private static Stream<LogData> parseFileToStream(
        Path filePath,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        String agentFilter
    ) {
        try {
            return Files.lines(filePath)
                .map(line -> parseLineToLogData(line, fromDate, toDate, agentFilter))
                .filter(Objects::nonNull);
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    /**
     * Parses a single line of a log file into a {@code LogData} object if it matches the specified filter.
     *
     * @param line        the line to parse
     * @param from        the start date-time for filtering a log
     * @param to          the end date-time for filtering a log
     * @param agentFilter the filter for matching specific user agents
     * @return the parsed {@code LogData} object, or {@code null} if the line doesn't match the criteria
     */
    private static LogData parseLineToLogData(String line, LocalDateTime from, LocalDateTime to, String agentFilter) {
        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.find()) {
            String currentLogTime = matcher.group(LogParams.TIMESTAMP.ordinal() + 1);
            LocalDateTime timestamp = LocalDateTime.parse(currentLogTime, LOG_DATE_FORMATTER);

            if ((from == null || !timestamp.isBefore(from)) && (to == null || !timestamp.isAfter(to))) {
                String resource = matcher.group(LogParams.REQUEST.ordinal() + 1).split(" ")[1];
                String responseCode = matcher.group(LogParams.STATUS.ordinal() + 1);
                long responseSize = Long.parseLong(matcher.group(LogParams.BODY_BYTES_SENT.ordinal() + 1));
                String ip = matcher.group(LogParams.REMOTE_ADDR.ordinal() + 1);
                String user = matcher.group(LogParams.REMOTE_USER.ordinal() + 1);
                String userAgent = matcher.group(LogParams.HTTP_USER_AGENT.ordinal() + 1);

                LogData logData = new LogData(ip, user, resource, responseCode, responseSize);
                if (isFollowAgentFilter(agentFilter, userAgent)) {
                    return logData;
                }
            }
        }
        return null;
    }

    /**
     * Checks whether the user agent matches the specified filter.
     *
     * @param agentFilter the filter for matching specific user agents
     * @param userAgent   the current user agent from the log to compare with the filter
     * @return {@code true} if the user agent matches the filter; {@code false} otherwise
     */
    private static boolean isFollowAgentFilter(String agentFilter, String userAgent) {
        if (agentFilter == null) {
            return true;
        }
        String regex = agentFilter.replace("*", ".*");
        Pattern pattern = Pattern.compile(regex);
        Matcher agentMatcher = pattern.matcher(userAgent);
        return agentMatcher.matches() || userAgent.equals(agentFilter);
    }

    /**
     * Creates a stream of {@code LogData} objects from a remote log URL.
     *
     * @param urlString   the URL to the remote log file
     * @param fromDate    the start date-time for filtering log entries
     * @param toDate      the end date-time for filtering log entries
     * @param agentFilter the filter for matching specific user agents
     * @return a stream of {@code LogData} objects parsed from the remote log
     */
    private static Stream<LogData> createStreamFromURL(
        String urlString,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        String agentFilter
    ) {
        try {
            URL url = new URI(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            return new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)).lines()
                .map(line -> parseLineToLogData(line, fromDate, toDate, agentFilter))
                .filter(Objects::nonNull);

        } catch (IOException | URISyntaxException e) {
            log.error("An error occurred while reading logs from the URL", e);
            return Stream.empty();
        }
    }

    /**
     * Validates whether the given string is a valid URL.
     *
     * @param url the string to validate
     * @return {@code true} if the string is a valid URL; {@code false} otherwise
     */
    private boolean isValidURL(String url) {
        try {
            new URI(url).toURL();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
