package backend.academy.logAnalyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class InputParser {
    @Getter private final List<String> paths;
    @Getter private LocalDateTime from;
    @Getter private LocalDateTime to;
    @Getter private String format;
    private final PrintStream output;
    private final BufferedReader reader;

    public InputParser(PrintStream output, InputStream input) {
        this.paths = new ArrayList<>();
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
                    String[] pathArgs = args[++pointer].split(",");
                    for (String pathArg : pathArgs) {
                        try {
                            URL url = new URI(pathArg).toURL();
                            paths.add(pathArg);
                        } catch (IllegalArgumentException | URISyntaxException | MalformedURLException e) {
                            if (isValidPath(pathArg)) {
                                Path localPath = Paths.get(pathArg);
                                paths.add(localPath.toString());
                            } else {
                                output.println("Invalid path: " + pathArg);
                            }
                        }
                    }
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

    private boolean isValidPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        String sanitizedPath = path.trim();
        if (!sanitizedPath.matches("[a-zA-Z0-9_/.-]+")) {
            return false;
        }
        try {
            Paths.get(sanitizedPath);
            return true;
        } catch (InvalidPathException e) {
            return false;
        }
    }

    public String readInputString() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            output.println("Your string is wrong!");
        }
        return null;
    }
}
