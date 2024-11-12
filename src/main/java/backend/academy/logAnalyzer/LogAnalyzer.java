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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogAnalyzer {
    private static final String UNDEFINED = "None";
    private static final int RESOURCE_ID = 4;
    private static final int RESPONSE_CODE_ID = 5;
    private static final int RESPONSE_SIZE_ID = 6;
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "^(\\S+) - (\\S+) \\[([^]]+)] "
            + "\"([^\"]+)\" (\\d{3}) (\\d+) "
            + "\"([^\"]*)\" \"([^\"]*)\"$"
    );
    private final PrintStream output;

    public LogAnalyzer(PrintStream output) {
        this.output = output;
    }

    public void analyze(String path, LocalDateTime fromDate, LocalDateTime toDate, String format) {
        List<LogData> logsData = new ArrayList<>();
        if (isValidURL(path)) {
            try (Stream<LogData> stream = createStreamFromURL(path, output)) {
                if (stream != null) {
                    logsData.addAll(stream.toList());
                }
            }
        } else {
            try {
                List<Path> logFiles = getMatchingFiles(path);
                logFiles.stream()
                    .flatMap(LogAnalyzer::parseFileToStream)
                    .forEach(logsData::add);
            } catch (IOException e) {
                output.println("IO exception!");
            }
        }
        processLogData(logsData, format);
    }

    private static List<Path> getMatchingFiles(String userPathPattern) throws IOException {
        String basePath = "src/main/resources/";  // Base directory for resources
        String fullPathPattern = basePath + userPathPattern;

        List<Path> logFiles = new ArrayList<>();
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + fullPathPattern);

        Files.walkFileTree(Paths.get(basePath + "logs"), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (pathMatcher.matches(file)) {
                    logFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return logFiles;
    }

    private static Stream<LogData> parseFileToStream(Path filePath) {
        try {
            return Files.lines(filePath)
                .map(LogAnalyzer::parseLineToLogData)
                .filter(Objects::nonNull);
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private static LogData parseLineToLogData(String line) {
        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.find()) {
            String resource = matcher.group(RESOURCE_ID);
            String responseCode = matcher.group(RESPONSE_CODE_ID);
            long responseSize = Long.parseLong(matcher.group(RESPONSE_SIZE_ID));
            return new LogData(resource, responseCode, responseSize);
        }
        return null;
    }

    private static Stream<LogData> createStreamFromURL(String urlString, PrintStream output) {
        try {
            URL url = new URI(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            return reader.lines().map(LogAnalyzer::parseLineToLogData).filter(Objects::nonNull);
        } catch (IOException | URISyntaxException e) {
            output.println("Error fetching or reading from URL: " + urlString);
            return Stream.empty();
        }
    }

    private void processLogData(List<LogData> logDataList, String format) {
        long totalRequests = logDataList.size();

        Map<String, Long> resourceFrequency = logDataList.stream()
            .collect(Collectors.groupingBy(LogData::resource, Collectors.counting()));
        String mostRequestedResource = resourceFrequency.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(UNDEFINED);

        Map<String, Long> responseCodeFrequency = logDataList.stream()
            .collect(Collectors.groupingBy(LogData::responseCode, Collectors.counting()));
        String mostFrequentResponseCode = responseCodeFrequency.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(UNDEFINED);

        double avgResponseSize = logDataList.stream()
            .mapToLong(LogData::responseSize)
            .average()
            .orElse(0.0);

        printResults(totalRequests, mostRequestedResource, mostFrequentResponseCode, avgResponseSize, output);
    }

    private void printResults(
        long totalRequests,
        String mostRequestedResource,
        String mostCommonResponseCode,
        double averageResponseSize,
        PrintStream output
    ) {
        output.println("Total Requests: " + totalRequests);
        output.println("Most Requested Resource: " + mostRequestedResource);
        output.println("Most Common Response Code: " + mostCommonResponseCode);
        output.println("Average Response Size: " + averageResponseSize);
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
