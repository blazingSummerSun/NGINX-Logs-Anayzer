package backend.academy.logAnalyzer;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class LogReportGenerator {
    private final String format;
    private final LocalDateTime fromDate;
    private final LocalDateTime toDate;
    private final double percentile;
    private final static String LOG_REPORT = "log_report";

    public LogReportGenerator(String format, LocalDateTime fromDate, LocalDateTime toDate, double percentile) {
        this.format = format;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.percentile = percentile;
    }

    public void generateLog(
        CollectedData collectedData, PrintStream output
    ) {
        String fileExtension;
        if (format == null || format.equals(FileExtensions.MARKDOWN.toString().toLowerCase())) {
            fileExtension = FileExtensions.MARKDOWN.extension();
            Path outputFile = Paths.get(LOG_REPORT + fileExtension);
            generateMarkdown(outputFile, collectedData, output);
        } else {
            fileExtension = FileExtensions.ASCIIDOC.extension();
            Path outputFile = Paths.get(LOG_REPORT + fileExtension);
            generateAsciiDoc(outputFile, collectedData, output);
        }
    }

    private void generateAsciiDoc(
        Path outputFile, CollectedData collectedData, PrintStream output
    ) {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8))) {
            writer.println("=== General Information");
            writer.println(AsciiDocStructure.TABLE.structure());
            writer.printf("| Metrics | Value %n");
            writer.println();
            writer.printf("| File(-s) | `%s` %n", String.join(", ", collectedData.fileNames()));
            writer.printf("| From date | %s %n", Objects.requireNonNullElse(fromDate, ""));
            writer.printf("| To date | %s %n", Objects.requireNonNullElse(toDate, ""));
            writer.printf("| Number of requests | %,d %n", collectedData.totalRequests());
            writer.printf("| Average response size | %,d b %n",
                collectedData.totalRequests() > 0 ? collectedData.totalResponseSize() / collectedData.totalRequests()
                    : 0);

            double calculatedPercentile = calculatePercentile(collectedData.responseSizes());
            writer.printf("| 95p answer size | %.2f b %n", calculatedPercentile);
            writer.println(AsciiDocStructure.TABLE.structure());

            writer.println();

            writer.println("=== Requested resources");
            writer.println(AsciiDocStructure.TABLE.structure());
            writer.println("| Resource | Amount ");
            writer.println();
            collectedData.resourceFrequency().forEach((resource, count) ->
                writer.printf("| `%s` | %,d %n", resource, count.get()));
            writer.println(AsciiDocStructure.TABLE.structure());

            writer.println();

            writer.println("=== Response codes");
            writer.println(AsciiDocStructure.TABLE.structure());
            writer.println();
            writer.println("| Code | Name | Amount ");

            collectedData.responseCodes().forEach((code, count) ->
                writer.printf("| %s | %s | %,d %n", code, getResponseCodeName(code), count));
            writer.println(AsciiDocStructure.TABLE.structure());

        } catch (IOException e) {
            output.println(ExceptionList.ERROR_WRITING_FILE.exception());
        }
    }

    private void generateMarkdown(
        Path outputFile, CollectedData collectedData, PrintStream output
    ) {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8))) {
            writer.println("### General Information");
            writer.println();
            writer.printf("| Metrics | Value |%n");
            writer.println("|-----------------------|------------------------------------|");
            writer.printf("| File(-s) | `%s` |%n", String.join(", ", collectedData.fileNames()));
            writer.printf("| From date | %s | %n", Objects.requireNonNullElse(fromDate, ""));
            writer.printf("| To date | %s | %n", Objects.requireNonNullElse(toDate, ""));
            writer.printf("| Number of requests | %,d |%n", collectedData.totalRequests());
            writer.printf("| Average response size | %,d b |%n",
                collectedData.totalRequests() > 0 ? collectedData.totalResponseSize() / collectedData.totalRequests()
                    : 0);

            double calculatedPercentile = calculatePercentile(collectedData.responseSizes());
            writer.printf("| 95p answer size | %.2f b |%n", calculatedPercentile);

            writer.println();

            writer.println("### Requested resources");
            writer.println();
            writer.println("| Resource | Amount |");
            writer.println("|------------------------|--------|");
            collectedData.resourceFrequency().forEach((resource, count) ->
                writer.printf("| `%s` | %,d |%n", resource, count.get()));

            writer.println();

            writer.println("### Response codes");
            writer.println();
            writer.println("| Code | Name | Amount |");
            writer.println("|------|------------------------|--------|");
            collectedData.responseCodes().forEach((code, count) ->
                writer.printf("| %s | %s | %,d |%n", code, getResponseCodeName(code), count));

        } catch (IOException e) {
            output.println(ExceptionList.ERROR_WRITING_FILE.exception());
        }
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

    private double calculatePercentile(List<Long> responseCodes) {
        if (responseCodes.isEmpty()) {
            return 0;
        }

        Collections.sort(responseCodes);

        int index = (int) Math.ceil(percentile * responseCodes.size()) - 1;
        return responseCodes.get(index);
    }
}
