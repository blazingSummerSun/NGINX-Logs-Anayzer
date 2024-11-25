package backend.academy.logAnalyzer.report;

import backend.academy.logAnalyzer.logs.CollectedData;
import backend.academy.logAnalyzer.logs.ResponseCodeNames;
import java.io.IOException;
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
import lombok.extern.slf4j.Slf4j;

/**
 * Generates log reports in Markdown or AsciiDoc format based on collected log data.
 */
@Slf4j public class LogReportGenerator {
    private static final String GENERAL_INFORMATION = " General Information";
    private static final String REQUESTED_RESOURCES = " Requested resources";
    private static final String RESPONSE_CODES = " Response codes";
    private final String format;
    private final LocalDateTime fromDate;
    private final LocalDateTime toDate;
    private static final String LOG_REPORT = "log_report";

    /**
     * Constructs a LogReportGenerator with the specified format, fromDate, and toDate.
     *
     * @param format   the format of the report (either "markdown" or "asciidoc").
     * @param fromDate the start date for the log data.
     * @param toDate   the end date for the log data.
     */
    public LogReportGenerator(String format, LocalDateTime fromDate, LocalDateTime toDate) {
        this.format = format;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    /**
     * Generates a log report based on the provided file names and collected data.
     *
     * @param fileNames     the names of the log files.
     * @param collectedData the collected log data.
     */
    public void generateLog(List<String> fileNames, CollectedData collectedData) {
        String fileExtension;
        if (format == null || format.equals(FileExtensions.MARKDOWN.toString().toLowerCase())) {
            fileExtension = FileExtensions.MARKDOWN.extension();
            Path outputFile = Paths.get(LOG_REPORT + fileExtension);
            generateMarkdown(fileNames, outputFile, collectedData);
        } else {
            fileExtension = FileExtensions.ASCIIDOC.extension();
            Path outputFile = Paths.get(LOG_REPORT + fileExtension);
            generateAsciiDoc(fileNames, outputFile, collectedData);
        }
    }

    /**
     * Generates a log report with .adoc extension.
     *
     * @param fileNames     the names of the log files.
     * @param outputFile    the path to the output file.
     * @param collectedData the collected log data.
     */
    private void generateAsciiDoc(
        List<String> fileNames,
        Path outputFile,
        CollectedData collectedData
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
            collectedData.resourceFrequency().entrySet()
                .stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue(Comparator.comparingLong(AtomicLong::get))
                    .reversed())
                .forEach(entry -> writer.printf("| %s | %,d %n", entry.getKey(), entry.getValue().get()));
            writer.println(AsciiDocStructure.TABLE.structure());

            writer.println();

            writer.println(AsciiDocStructure.HEADER.structure() + RESPONSE_CODES);
            writer.println(AsciiDocStructure.TABLE.structure());
            writer.println();
            writer.println("| Code | Name | Amount ");

            collectedData.responseCodes().entrySet()
                .stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue(Comparator.comparingLong(AtomicLong::get))
                    .reversed())
                .forEach(
                    entry -> writer.printf("| %s | %s | %,d %n", entry.getKey(), getResponseCodeName(entry.getKey()),
                        entry.getValue().get()));
            writer.println(AsciiDocStructure.TABLE.structure());

        } catch (IOException e) {
            log.error("An error occurred while writing to the .adoc file");
        }
    }

    /**
     * Generates a log report with .md extension.
     *
     * @param fileNames     the names of the log files.
     * @param outputFile    the path to the output file.
     * @param collectedData the collected log data.
     */
    private void generateMarkdown(
        List<String> fileNames,
        Path outputFile,
        CollectedData collectedData
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
            collectedData.resourceFrequency().entrySet()
                .stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue(Comparator.comparingLong(AtomicLong::get))
                    .reversed())
                .forEach(entry -> writer.printf("| %s | %,d |%n", entry.getKey(), entry.getValue().get()));

            writer.println();

            writer.println(MarkdownStructure.HEADER.structure() + RESPONSE_CODES);
            writer.println();
            writer.println("| Code | Name | Amount |");
            writer.println(MarkdownStructure.SPLITERATOR_3.structure());
            collectedData.responseCodes().entrySet()
                .stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue(Comparator.comparingLong(AtomicLong::get))
                    .reversed())
                .forEach(
                    entry -> writer.printf("| %s | %s | %,d |%n", entry.getKey(), getResponseCodeName(entry.getKey()),
                        entry.getValue().get()));

        } catch (IOException e) {
            log.error("An error occurred while writing to the .md file");
        }
    }

    /**
     * Searches for the most frequent user in the log files.
     *
     * @param users a map stores the frequency of each user.
     * @return the most frequent user.
     */
    private String theMostFrequentUser(Map<String, AtomicLong> users) {
        return users.entrySet().stream()
            .max(Map.Entry.comparingByValue(Comparator.comparingLong(AtomicLong::get)))
            .map(Map.Entry::getKey)
            .orElse("");
    }

    /**
     * Searches for the most frequent IP in the log files.
     *
     * @param ips a map stores the frequency of each ip.
     * @return the most frequent ip.
     */
    private String theMostFrequentIp(Map<String, AtomicLong> ips) {
        return ips.entrySet().stream()
            .max(Map.Entry.comparingByValue(Comparator.comparingLong(AtomicLong::get)))
            .map(Map.Entry::getKey)
            .orElse("");
    }

    /**
     * Defines the type of each response code.
     *
     * @param code a string representing the response code of the specific log.
     * @return the type of the response code.
     */
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
