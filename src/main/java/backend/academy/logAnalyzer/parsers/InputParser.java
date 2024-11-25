package backend.academy.logAnalyzer.parsers;

import backend.academy.logAnalyzer.exceptions.CorruptedInputStringException;
import backend.academy.logAnalyzer.exceptions.EmptyInputStringException;
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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Parses input data from a provided input stream.
 * The class reads the input string and parses the data to identify the path, date range, format, and filter by agent.
 */
@Slf4j public class InputParser {
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
     * @throws EmptyInputStringException if the input string is empty.
     */
    public void parseData(String inputString) {
        if (inputString.isEmpty()) {
            throw new EmptyInputStringException("input string is empty");
        }
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
                        output.println("Such a filter doesn't exist!");
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
     * @return the validated path string.
     * @throws InvalidPathException if the path has invalid symbols.
     */
    private String parsePath(String arg) {
        try {
            new URI(arg).toURL();
            return arg;
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
            if (isValidPathOrPattern(arg)) {
                return arg;
            } else {
                InvalidPathException invalidPathException =
                    new InvalidPathException(arg, "input path has incorrect format");
                invalidPathException.addSuppressed(e);
                throw invalidPathException;
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
        return sanitizedPath.matches("[a-zA-Z0-9_/.*?-]+");
    }

    /**
     * Reads an input string from the input stream.
     *
     * @return the input string.
     * @throws CorruptedInputStringException if the input string has been corrupted.
     */
    public String readInputString() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            CorruptedInputStringException corruptedInputStringException =
                new CorruptedInputStringException("input string has been corrupted");
            corruptedInputStringException.addSuppressed(e);
            throw corruptedInputStringException;
        }
    }
}
