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
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

    public void analyze(String path, LocalDateTime fromDate, LocalDateTime toDate, String format) {
        AtomicLong totalRequests = new AtomicLong();
        Map<String, AtomicLong> resourceFrequency = new ConcurrentHashMap<>();
        Map<String, AtomicLong> responseCodeFrequency = new ConcurrentHashMap<>();
        AtomicLong totalResponseSize = new AtomicLong();
        List<String> processedFiles = new ArrayList<>();
        List<Long> responseSizes = new ArrayList<>();

        Supplier<Stream<LogData>> logDataStreamSupplier = () -> {
            if (isValidURL(path)) {
                processedFiles.add(path);
                return createStreamFromURL(path, output, fromDate, toDate);
            } else {
                List<Path> logFiles = getMatchingFiles(path, output);
                logFiles.forEach(file -> processedFiles.add(file.toString()));
                return logFiles.stream().flatMap(file -> parseFileToStream(file, fromDate, toDate));

            }
        };

        try (Stream<LogData> logDataStream = logDataStreamSupplier.get()) {
            logDataStream.forEach(log -> {
                totalRequests.incrementAndGet();
                resourceFrequency.computeIfAbsent(log.resource(), k -> new AtomicLong()).incrementAndGet();
                responseCodeFrequency.computeIfAbsent(log.responseCode(), k -> new AtomicLong()).incrementAndGet();
                totalResponseSize.addAndGet(log.responseSize());
                responseSizes.add(log.responseSize());
            });
        }

        Map<String, Long> filteredResponseCodes = responseCodeFrequency.entrySet().stream()
            .filter(entry -> entry.getValue().get() > 0)
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));

        CollectedData collectedData = new CollectedData(processedFiles, totalRequests.get(), resourceFrequency,
            filteredResponseCodes, totalResponseSize.get(), responseSizes);
        LogReportGenerator logReport = new LogReportGenerator(format, fromDate, toDate, PERCENTILE);
        logReport.generateLog(collectedData, output);
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
        } catch (IOException e) {
            output.println("path doesn't exist!");
        }
        return logFiles;
    }

    private static Stream<LogData> parseFileToStream(Path filePath, LocalDateTime fromDate, LocalDateTime toDate) {
        try {
            return Files.lines(filePath)
                .map(line -> parseLineToLogData(line, fromDate, toDate))
                .filter(Objects::nonNull);
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private static LogData parseLineToLogData(String line, LocalDateTime from, LocalDateTime to) {
        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.find()) {
            String currentLogTime = matcher.group(LogParams.TIMESTAMP.ordinal() + SHIFT);
            LocalDateTime timestamp = LocalDateTime.parse(currentLogTime, LOG_DATE_FORMATTER);

            if ((from == null || !timestamp.isBefore(from)) && (to == null || !timestamp.isAfter(to))) {
                String resource = matcher.group(LogParams.REQUEST.ordinal() + SHIFT).split(" ")[1];
                String responseCode = matcher.group(LogParams.STATUS.ordinal() + SHIFT);
                long responseSize = Long.parseLong(matcher.group(LogParams.BODY_BYTES_SENT.ordinal() + SHIFT));
                return new LogData(resource, responseCode, responseSize);
            }
        }
        return null;
    }

    private static Stream<LogData> createStreamFromURL(
        String urlString,
        PrintStream output,
        LocalDateTime fromDate,
        LocalDateTime toDate
    ) {
        try {
            URL url = new URI(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            return reader.lines()
                .map(line -> parseLineToLogData(line, fromDate, toDate)).filter(Objects::nonNull);
        } catch (IOException | URISyntaxException e) {
            output.println("Error fetching or reading from URL: " + urlString);
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
