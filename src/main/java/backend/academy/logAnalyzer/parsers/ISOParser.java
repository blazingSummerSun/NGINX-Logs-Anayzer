package backend.academy.logAnalyzer.parsers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Parses ISO8601 formatted date strings into LocalDateTime objects.
 */
public class ISOParser {
    private static final String DATE_TIME_WITH_ZONE = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}([+-]\\d{2}:\\d{2}|Z)";
    private static final String BASIC_DATE_TIME_WITH_ZONE = "\\d{8}T\\d{6}Z";
    private static final String LOCAL_DATE_OR_ORDINAL = "\\d{4}(-\\d{2}-\\d{2}|-\\d{3})";
    private static final String WEEK_DATE = "\\d{4}-W\\d{2}(-\\d)?";
    private static final String MONTH_DAY = "--\\d{2}-\\d{2}";
    private static final int LOCAL_DATE_LENGTH = 10;

    /**
     * Parses an ISO 8601 formatted date string into a LocalDateTime object.
     *
     * @param isoDate the ISO8601 formatted date string.
     * @return the corresponding LocalDateTime object, or null if the input doesn't match the ISO8601 pattern.
     */
    public LocalDateTime parseIso8601(String isoDate) {
        LocalDateTime formattedDate;
        if (isoDate.matches(DATE_TIME_WITH_ZONE)) {
            formattedDate = parseDateTimeWithZone(isoDate);
        } else if (isoDate.matches(BASIC_DATE_TIME_WITH_ZONE)) {
            formattedDate = parseBasicDateTimeWithZone(isoDate);
        } else if (isoDate.matches(LOCAL_DATE_OR_ORDINAL)) {
            formattedDate = parseLocalDateOrOrdinal(isoDate);
        } else if (isoDate.matches(WEEK_DATE)) {
            formattedDate = parseWeekDate(isoDate);
        } else if (isoDate.matches(MONTH_DAY)) {
            formattedDate = parseMonthDay(isoDate);
        } else {
            return null;
        }
        return formattedDate;
    }

    private LocalDateTime parseDateTimeWithZone(String isoDate) {
        try {
            if (isoDate.endsWith("Z")) {
                return ZonedDateTime.parse(isoDate, DateTimeFormatter.ISO_ZONED_DATE_TIME).toLocalDateTime();
            } else {
                return OffsetDateTime.parse(isoDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
            }
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDateTime parseBasicDateTimeWithZone(String isoDate) {
        try {
            return ZonedDateTime.parse(isoDate, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDateTime parseLocalDateOrOrdinal(String isoDate) {
        try {
            if (isoDate.contains("-")) {
                if (isoDate.length() == LOCAL_DATE_LENGTH) { // YYYY-MM-DD
                    return LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
                } else { // YYYY-DDD
                    return LocalDate.parse(isoDate, DateTimeFormatter.ISO_ORDINAL_DATE).atStartOfDay();
                }
            }
        } catch (DateTimeParseException ignored) {
            return null;
        }
        return null;
    }

    private LocalDateTime parseWeekDate(String isoDate) {
        try {
            // Set the specific day (Monday) if not provided
            String isoDateCopy = isoDate;
            if (isoDate.matches("\\d{4}-W\\d{2}")) {
                isoDateCopy += "-1";
            }
            return LocalDate.parse(isoDateCopy, DateTimeFormatter.ISO_WEEK_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDateTime parseMonthDay(String isoDate) {
        try {
            MonthDay monthDay = MonthDay.parse(isoDate, DateTimeFormatter.ofPattern("--MM-dd"));
            return monthDay.atYear(Year.now().getValue()).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
