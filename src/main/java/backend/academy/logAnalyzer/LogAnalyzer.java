package backend.academy.logAnalyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class LogAnalyzer {
    private final InputParser inputParser;
    private final PrintStream output;
    private static final String UNDEFINED = "None";
    private static final int RESOURCE_ID = 4;
    private static final int RESPONSE_CODE_ID = 5;
    private static final int RESPONSE_SIZE_ID = 6;

    public LogAnalyzer(InputParser inputParser, PrintStream output) {
        this.inputParser = inputParser;
        this.output = output;

    }

    public void analyze() {
        AtomicLong totalRequests = new AtomicLong();
        Map<String, Integer> resourceCount = new HashMap<>();
        Map<String, Integer> responseCodeCount = new HashMap<>();
        AtomicLong totalResponseSize = new AtomicLong(0);
        for (String path : inputParser.paths()) {
            try (Stream<String> stream = createStream(path)) {
                if (stream != null) {
                    stream.forEach(line -> {
                        totalRequests.getAndIncrement();
                        LogData logEntry = parseLogEntry(line);

                        resourceCount.put(logEntry.resource(),
                            resourceCount.getOrDefault(logEntry.resource(), 0) + 1);

                        responseCodeCount.put(logEntry.responseCode(),
                            responseCodeCount.getOrDefault(logEntry.responseCode(), 0) + 1);

                        totalResponseSize.addAndGet(logEntry.responseSize());
                    });
                }
            }
        }
        String mostRequestedResource = resourceCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(UNDEFINED);
        String mostCommonResponseCode = responseCodeCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(UNDEFINED);
        double averageResponseSize =
            totalRequests.get() > 0 ? (double) totalResponseSize.get() / totalRequests.get() : 0;

        output.println("Total Requests: " + totalRequests);
        output.println("Most Requested Resource: " + mostRequestedResource);
        output.println("Most Common Response Code: " + mostCommonResponseCode);
        output.println("Average Response Size: " + averageResponseSize);
    }

    private Stream<String> createStream(String path) {
        try {
            if (isValidURL(path)) {
                URL url = new URI(path).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    return reader.lines();
                }
            } else {
                return Files.lines(Paths.get(path));
            }
        } catch (URISyntaxException e) {
            output.println("Error in syntax!");
        } catch (MalformedURLException e) {
            output.println("Wrong url!");
        } catch (IOException e) {
            output.println("IO exception");
        }
        return null;
    }

    private LogData parseLogEntry(String line) {
        String logPattern =
            "^(\\S+) - (\\S+) \\[([^]]+)] "
                + "\"([^\"]+)\" (\\d{3}) (\\d+) "
                + "\"([^\"]*)\" \"([^\"]*)\"$";

        Pattern pattern = Pattern.compile(logPattern);
        Matcher matcher = pattern.matcher(line);

        if (matcher.matches()) {
            String resource = matcher.group(RESOURCE_ID).split(" ")[1];
            String responseCode = matcher.group(RESPONSE_CODE_ID);
            long responseSize = Long.parseLong(matcher.group(RESPONSE_SIZE_ID));

            return new LogData(resource, responseCode, responseSize);
        } else {
            throw new IllegalArgumentException("Log line doesn't match the NGINX format");
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
