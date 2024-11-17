package backend.academy.logAnalyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * Parses input data from a provided input stream.
 * The class reads the input string and parses the data to identify the path, date range, format, and filter by agent.
 */
public class InputParser {
    private static final String AGENT_FILTER = "agent";
    @Getter private String path;
    @Getter private LocalDateTime from;
    @Getter private LocalDateTime to;
    @Getter private String format;
    @Getter private String agentValue;
    private boolean agentFilter;
    private final PrintStream output;
    private final BufferedReader reader;

    /**
     * Constructs an InputParser with the specified output and input streams.
     *
     * @param output the PrintStream to output messages.
     * @param input  the InputStream to read input data.
     */
    public InputParser(PrintStream output, InputStream input) {
        this.agentFilter = false;
        this.output = output;
        this.reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
    }

    /**
     * Parses the input data and extracts parameters.
     */
    public void parseData() {
        String inputString = readInputString();
        String[] args = inputString.split(" ");
        ISOParser isoParser = new ISOParser();
        int pointer = 0;

        while (pointer < args.length - 1) {
            switch (args[pointer]) {
                case "--path":
                    path = parsePath(args[++pointer]);
                    break;
                case "--from":
                    this.from = isoParser.parseIso8601(args[++pointer]);
                    break;
                case "--to":
                    this.to = isoParser.parseIso8601(args[++pointer]);
                    break;
                case "--format":
                    this.format = args[++pointer];
                    break;
                case "--filter-field":
                    if (AGENT_FILTER.equals(args[++pointer])) {
                        agentFilter = true;
                    } else {
                        output.println(ExceptionList.INVALID_FILTER_FIELD.exception());
                        pointer++;
                    }
                    break;
                case "--filter-value":
                    if (agentFilter) {
                        StringBuilder filteredString = new StringBuilder(args[++pointer].substring(1));
                        while (pointer < args.length - 1 && !args[++pointer].startsWith("--")) {
                            filteredString.append(" ").append(args[pointer]);
                        }
                        this.agentValue = filteredString.substring(0, filteredString.length() - 1);
                    } else {
                        pointer++;
                    }
                    break;
                default:
                    pointer++;
            }
        }
    }

    /**
     * Parses and validates the provided path argument.
     *
     * @param arg the path argument to be parsed.
     * @return the validated path string, or null if invalid.
     */
    private String parsePath(String arg) {
        try {
            new URI(arg).toURL();
            return arg;
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
            if (isValidPathOrPattern(arg)) {
                return arg;
            } else {
                output.println(ExceptionList.INVALID_PATH_PATTERN.exception());
                return null;
            }
        }
    }

    /**
     * Checks if the provided path or pattern is valid.
     *
     * @param path the path or pattern to be validated.
     * @return true if valid, false otherwise.
     */
    private boolean isValidPathOrPattern(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        String sanitizedPath = path.trim();
        if (!sanitizedPath.matches("[a-zA-Z0-9_/.*?-]+")) {
            return false;
        }
        try {
            Paths.get(sanitizedPath);
            return true;
        } catch (InvalidPathException e) {
            return false;
        }
    }

    /**
     * Reads an input string from the input stream.
     *
     * @return the input string, or null if an error occurs.
     */
    public String readInputString() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            output.println(ExceptionList.ERROR_INPUT_STRING.exception());
        }
        return null;
    }
}
