package backend.academy.logAnalyzer;

import backend.academy.logAnalyzer.logs.CollectedData;
import backend.academy.logAnalyzer.logs.LogAnalyzer;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogAnalyzerTest {
    @Test
    void shouldConsiderTenLogLinesLocal() {
        LogAnalyzer logAnalyzer = new LogAnalyzer();
        CollectedData collectedData = logAnalyzer.analyze("logs/10LinesTest.txt", null, null, null);
        assertEquals(10, collectedData.totalRequests());
    }

    @Test
    void shouldSplitLogToEightParts() {
        String log = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET /downloads/product_1 HTTP/1.1\" 304 0 \"-\" " +
            "\"Debian APT-HTTP/1.3 (0.8.16~exp12ubuntu10.21)\"";
        Pattern logPattern = Pattern.compile(
            "^(\\S+) - (\\S+) \\[([^]]+)] "
                + "\"([^\"]+)\" (\\d{3}) (\\d+) "
                + "\"([^\"]*)\" \"([^\"]*)\"$"
        );
        Matcher matcher = logPattern.matcher(log);
        assertTrue(matcher.find());
    }

    @Test
    void shouldConsiderFiveLogLinesLocal() {
        LogAnalyzer logAnalyzer = new LogAnalyzer();
        DateTimeFormatter localDateFormatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");
        LocalDateTime fromDate = LocalDateTime.parse("17/May/2019:08:05:32 +0000", localDateFormatter);
        LocalDateTime toDate = LocalDateTime.parse("17/May/2022:08:05:32 +0000", localDateFormatter);
        CollectedData collectedData = logAnalyzer.analyze("logs/10LinesTest.txt", fromDate, toDate, null);
        assertEquals(5, collectedData.totalRequests());
    }

    @Test
    void shouldConsiderTwoLogLinesLocal() {
        LogAnalyzer logAnalyzer = new LogAnalyzer();
        DateTimeFormatter localDateFormatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");
        LocalDateTime toDate = LocalDateTime.parse("17/May/2011:08:05:32 +0000", localDateFormatter);
        CollectedData collectedData = logAnalyzer.analyze("logs/10LinesTest.txt", null, toDate, null);
        assertEquals(2, collectedData.totalRequests());
    }

    @Test
    void shouldConsiderEightLogLinesLocal() {
        LogAnalyzer logAnalyzer = new LogAnalyzer();
        DateTimeFormatter localDateFormatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");
        LocalDateTime fromDate = LocalDateTime.parse("17/May/2012:08:05:32 +0000", localDateFormatter);
        CollectedData collectedData = logAnalyzer.analyze("logs/10LinesTest.txt", fromDate, null, null);
        assertEquals(8, collectedData.totalRequests());
    }

    @Test
    void shouldReturn3DifferentResponseCodes() {
        LogAnalyzer logAnalyzer = new LogAnalyzer();
        CollectedData collectedData = logAnalyzer.analyze("logs/10LinesTest.txt", null, null, null);
        assertEquals(3, collectedData.responseCodes().size());
    }

    @Test
    void shouldReturn5() {
        LogAnalyzer logAnalyzer = new LogAnalyzer();
        CollectedData collectedData = logAnalyzer.analyze("logs/10LinesTest.txt", null, null, null);
        assertEquals(5, collectedData.users().get("usr").get());
    }

    @Test
    void shouldReturn8TimesProduct1() {
        LogAnalyzer logAnalyzer = new LogAnalyzer();
        CollectedData collectedData = logAnalyzer.analyze("logs/10LinesTest.txt", null, null, null);
        assertEquals(8, collectedData.resourceFrequency().get("/downloads/product_1").get());
    }

    @Test
    void shouldReturn490AsPercentile() {
        LogAnalyzer logAnalyzer = new LogAnalyzer();
        CollectedData collectedData = logAnalyzer.analyze("logs/10LinesTest.txt", null, null, null);
        assertEquals(490.00, collectedData.percentile());
    }
}
