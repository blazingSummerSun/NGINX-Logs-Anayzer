package backend.academy.logAnalyzer;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class LogReportGenerator {
    private static final String GENERAL_INFORMATION = " General Information";
    private static final String REQUESTED_RESOURCES = " Requested resources";
    private static final String RESPONSE_CODES = " Response codes";
    private final String format;
    private final LocalDateTime fromDate;
    private final LocalDateTime toDate;
    private static final String LOG_REPORT = "log_report";

    public LogReportGenerator(String format, LocalDateTime fromDate, LocalDateTime toDate) {
        this.format = format;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public void generateLog(List<String> fileNames, CollectedData collectedData, PrintStream output) {
        String fileExtension;
        if (format == null || format.equals(FileExtensions.MARKDOWN.toString().toLowerCase())) {
            fileExtension = FileExtensions.MARKDOWN.extension();
            Path outputFile = Paths.get(LOG_REPORT + fileExtension);
            generateMarkdown(fileNames, outputFile, collectedData, output);
        } else {
            fileExtension = FileExtensions.ASCIIDOC.extension();
            Path outputFile = Paths.get(LOG_REPORT + fileExtension);
            generateAsciiDoc(fileNames, outputFile, collectedData, output);
        }
    }

    private void generateAsciiDoc(
        List<String> fileNames,
        Path outputFile,
        CollectedData collectedData,
        PrintStream output
    ) {
        String frequentIp = theMostFrequentIp(collectedData.ips());
        String frequentUser = theMostFrequentUser(collectedData.users());
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8))) {
            writer.println(AsciiDocStructure.HEADER.structure() + GENERAL_INFORMATION);
            writer.println(AsciiDocStructure.TABLE.structure());
            writer.printf("| Metrics | Value %n");
            writer.println();
            writer.printf("| File(-s) | %s%n", String.join(", ", fileNames));
            writer.printf("| From date | %s %n", Objects.requireNonNullElse(fromDate, "-"));
            writer.printf("| To date | %s %n", Objects.requireNonNullElse(toDate, "-"));
            writer.printf("| Number of requests | %,d %n", collectedData.totalRequests());
            writer.printf("| Average response size | %,d b %n",
                collectedData.totalRequests() > 0 ? collectedData.totalResponseSize() / collectedData.totalRequests()
                    : 0);

            writer.printf("| 95p answer size | %.2f b %n", collectedData.percentile());
            writer.printf("| Most frequent IP | %s %n", frequentIp);
            writer.printf("| Most frequent user | %s %n", frequentUser);
            writer.println(AsciiDocStructure.TABLE.structure());

            writer.println();

            writer.println(AsciiDocStructure.HEADER.structure() + REQUESTED_RESOURCES);
            writer.println(AsciiDocStructure.TABLE.structure());
            writer.println("| Resource | Amount ");
            writer.println();
            collectedData.resourceFrequency().forEach((resource, count) ->
                writer.printf("| `%s` | %,d %n", resource, count.get()));
            writer.println(AsciiDocStructure.TABLE.structure());

            writer.println();

            writer.println(AsciiDocStructure.HEADER.structure() + RESPONSE_CODES);
            writer.println(AsciiDocStructure.TABLE.structure());
            writer.println();
            writer.println("| Code | Name | Amount ");

            collectedData.responseCodes().forEach((code, count) ->
                writer.printf("| %s | %s | %,d %n", code, getResponseCodeName(code), count.get()));
            writer.println(AsciiDocStructure.TABLE.structure());

        } catch (IOException e) {
            output.println(ExceptionList.ERROR_WRITING_FILE.exception());
        }
    }

    private void generateMarkdown(
        List<String> fileNames,
        Path outputFile,
        CollectedData collectedData,
        PrintStream output
    ) {
        String frequentIp = theMostFrequentIp(collectedData.ips());
        String frequentUser = theMostFrequentUser(collectedData.users());
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8))) {
            writer.println(MarkdownStructure.HEADER.structure() + GENERAL_INFORMATION);
            writer.println();
            writer.printf("| Metrics | Value |%n");
            writer.println(MarkdownStructure.SPLITERATOR_2.structure());
            writer.printf("| File(-s) | `%s` |%n", String.join(", ", fileNames));
            writer.printf("| From date | %s | %n", Objects.requireNonNullElse(fromDate, "-"));
            writer.printf("| To date | %s | %n", Objects.requireNonNullElse(toDate, "-"));
            writer.printf(
                "| Number of requests | %,d |%n", collectedData.totalRequests());
            writer.printf("| Average response size | %,d b |%n",
                collectedData.totalRequests() > 0 ? collectedData.totalResponseSize() / collectedData.totalRequests()
                    : 0);

            writer.printf("| 95p answer size | %.2f b |%n", collectedData.percentile());
            writer.printf("| Most frequent IP | %s |%n", frequentIp);
            writer.printf("| Most frequent user | %s |%n", frequentUser);

            writer.println();

            writer.println(MarkdownStructure.HEADER.structure() + REQUESTED_RESOURCES);
            writer.println();
            writer.println("| Resource | Amount |");
            writer.println(MarkdownStructure.SPLITERATOR_2.structure());
            collectedData.resourceFrequency().forEach((resource, count) ->
                writer.printf("| `%s` | %,d |%n", resource, count.get()));

            writer.println();

            writer.println(MarkdownStructure.HEADER.structure() + RESPONSE_CODES);
            writer.println();
            writer.println("| Code | Name | Amount |");
            writer.println(MarkdownStructure.SPLITERATOR_3.structure());
            collectedData.responseCodes().forEach((code, count) ->
                writer.printf("| %s | %s | %,d |%n", code, getResponseCodeName(code), count.get()));

        } catch (IOException e) {
            output.println(ExceptionList.ERROR_WRITING_FILE.exception());
        }
    }

    private String theMostFrequentUser(Map<String, AtomicLong> users) {
        return users.entrySet().stream()
            .max(Map.Entry.comparingByValue(Comparator.comparingLong(AtomicLong::get)))
            .map(Map.Entry::getKey)
            .orElse("");
    }

    private String theMostFrequentIp(Map<String, AtomicLong> ids) {
        return ids.entrySet().stream()
            .max(Map.Entry.comparingByValue(Comparator.comparingLong(AtomicLong::get)))
            .map(Map.Entry::getKey)
            .orElse("");
    }

    private String getResponseCodeName(String code) {
        int intRepresentation = Integer.parseInt(code);
        String returnCode;
        if (intRepresentation >= Integer.parseInt(ResponseCodeNames.SERVER_ERROR_RESPONSES.responseCode())) {
            returnCode = "Server error responses";
        } else if (intRepresentation >= Integer.parseInt(ResponseCodeNames.CLIENT_ERROR_RESPONSES.responseCode())) {
            returnCode = "Client error responses";
        } else if (intRepresentation >= Integer.parseInt(ResponseCodeNames.REDIRECT_RESPONSES.responseCode())) {
            returnCode = "Redirection messages";
        } else if (intRepresentation >= Integer.parseInt(ResponseCodeNames.SUCCESS_RESPONSES.responseCode())) {
            returnCode = "Successful responses";
        } else {
            returnCode = "Informational responses";
        }
        return returnCode;
    }
}
