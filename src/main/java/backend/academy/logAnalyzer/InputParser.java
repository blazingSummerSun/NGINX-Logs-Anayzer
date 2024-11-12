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

public class InputParser {
    @Getter private String path;
    @Getter private LocalDateTime from;
    @Getter private LocalDateTime to;
    @Getter private String format;
    private final PrintStream output;
    private final BufferedReader reader;

    public InputParser(PrintStream output, InputStream input) {
        this.output = output;
        this.reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
    }

    public void parseData() {
        String inputString = readInputString();
        String[] args = inputString.split(" ");
        ISOParser isoParser = new ISOParser();
        int pointer = 0;

        while (pointer < args.length) {
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
                default:
                    pointer++;
            }
        }
    }

    private String parsePath(String arg) {
        try {
            new URI(arg).toURL();
            return arg;  // It's a valid URL
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
            if (isValidPathOrPattern(arg)) {
                return arg;
            } else {
                output.println("Invalid path or pattern: " + arg);
                return null;
            }
        }
    }

    private boolean isValidPathOrPattern(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        String sanitizedPath = path.trim();
        if (!sanitizedPath.matches("[a-zA-Z0-9_/.*?-]+")) {
            return false;
        }
        try {
            Paths.get(sanitizedPath.replace("*", "test"));  // Replace '*' temporarily to check path validity
            return true;
        } catch (InvalidPathException e) {
            return false;
        }
    }

    public String readInputString() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            output.println("Error reading input!");
        }
        return null;
    }
}
