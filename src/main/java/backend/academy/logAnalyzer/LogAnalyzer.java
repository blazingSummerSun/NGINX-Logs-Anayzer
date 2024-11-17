package backend.academy.logAnalyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
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

public class LogAnalyzer {
    private static final double PERCENTILE = 0.95;
    private static final int SHIFT = 1;
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "^(\\S+) - (\\S+) \\[([^]]+)] "
            + "\"([^\"]+)\" (\\d{3}) (\\d+) "
            + "\"([^\"]*)\" \"([^\"]*)\"$"
    );
    private static final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");
    private final PrintStream output;

    public LogAnalyzer(PrintStream output) {
        this.output = output;
    }

    public void analyze(String path, LocalDateTime fromDate, LocalDateTime toDate, String format, String agentFilter) {
        AtomicLong totalRequests = new AtomicLong();
        Map<String, AtomicLong> ips = new ConcurrentHashMap<>();
        Map<String, AtomicLong> users = new ConcurrentHashMap<>();
        Map<String, AtomicLong> resourceFrequency = new ConcurrentHashMap<>();
        Map<String, AtomicLong> responseCodeFrequency = new ConcurrentHashMap<>();
        AtomicLong totalResponseSize = new AtomicLong();
        List<String> processedFiles = new ArrayList<>();
        List<Long> responseSizes = new ArrayList<>();

        Supplier<Stream<LogData>> logDataStreamSupplier = () -> {
            if (isValidURL(path)) {
                processedFiles.add(path);
                return createStreamFromURL(path, output, fromDate, toDate, agentFilter);
            } else {
                List<Path> logFiles = getMatchingFiles(path, output);
                logFiles.forEach(file -> processedFiles.add(file.toString()));
                return logFiles.stream().flatMap(file -> parseFileToStream(file, fromDate, toDate, agentFilter));

            }
        };

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
        CollectedData collectedData = new CollectedData(totalRequests.get(), resourceFrequency,
            responseCodeFrequency, totalResponseSize.get(), responseSizes, ips, users, percentile);
        LogReportGenerator logReport = new LogReportGenerator(format, fromDate, toDate);
        logReport.generateLog(processedFiles, collectedData, output);
    }

    private static List<Path> getMatchingFiles(String userPathPattern, PrintStream output) {
        String basePath = "src/main/resources/";
        String fullPathPattern = basePath + userPathPattern;

        List<Path> logFiles = new ArrayList<>();
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + fullPathPattern);

        try {
            Files.walkFileTree(Paths.get(basePath + "logs"), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (pathMatcher.matches(file)) {
                        logFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (InvalidPathException | NoSuchFileException e) {
            output.println(ExceptionList.INVALID_PATH_PATTERN.exception());
        } catch (IOException e) {
            output.println(ExceptionList.ERROR_WRITING_FILE.exception());
        }
        return logFiles;
    }

    private double calculatePercentile(List<Long> responseCodes) {
        if (responseCodes.isEmpty()) {
            return 0;
        }

        Collections.sort(responseCodes);

        int index = (int) Math.ceil(PERCENTILE * responseCodes.size()) - 1;
        return responseCodes.get(index);
    }

    private static Stream<LogData> parseFileToStream(
        Path filePath,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        String agentFilter
    ) {
        try (Stream<String> lines = Files.lines(filePath)) {
            return lines
                .map(line -> parseLineToLogData(line, fromDate, toDate, agentFilter))
                .filter(Objects::nonNull);
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private static LogData parseLineToLogData(String line, LocalDateTime from, LocalDateTime to, String agentFilter) {
        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.find()) {
            String currentLogTime = matcher.group(LogParams.TIMESTAMP.ordinal() + SHIFT);
            LocalDateTime timestamp = LocalDateTime.parse(currentLogTime, LOG_DATE_FORMATTER);

            if ((from == null || !timestamp.isBefore(from)) && (to == null || !timestamp.isAfter(to))) {
                String resource = matcher.group(LogParams.REQUEST.ordinal() + SHIFT).split(" ")[1];
                String responseCode = matcher.group(LogParams.STATUS.ordinal() + SHIFT);
                long responseSize = Long.parseLong(matcher.group(LogParams.BODY_BYTES_SENT.ordinal() + SHIFT));
                String ip = matcher.group(LogParams.REMOTE_ADDR.ordinal() + SHIFT);
                String user = matcher.group(LogParams.REMOTE_USER.ordinal() + SHIFT);
                String userAgent = matcher.group(LogParams.HTTP_USER_AGENT.ordinal() + SHIFT);

                LogData logData = new LogData(ip, user, resource, responseCode, responseSize);
                if (isFollowAgentFilter(agentFilter, userAgent)) {
                    return logData;
                }
            }
        }
        return null;
    }

    private static boolean isFollowAgentFilter(String agentFilter, String userAgent) {
        if (agentFilter == null) {
            return true;
        }
        String regex = agentFilter.replace("*", ".*");
        Pattern pattern = Pattern.compile(regex);
        Matcher agentMatcher = pattern.matcher(userAgent);
        return agentMatcher.matches() || userAgent.equals(agentFilter);
    }

    private static Stream<LogData> createStreamFromURL(
        String urlString,
        PrintStream output,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        String agentFilter
    ) {
        try {
            URL url = new URI(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines()
                    .map(line -> parseLineToLogData(line, fromDate, toDate, agentFilter))
                    .filter(Objects::nonNull);
            }
        } catch (IOException | URISyntaxException e) {
            output.println(ExceptionList.ERROR_FETCHING_URL.exception());
            return Stream.empty();
        }
    }

    private boolean isValidURL(String url) {
        try {
            new URI(url).toURL();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
