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
    private static final String ISO_OFFSET_DATE_TIME = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}";
    private static final String ISO_ZONED_DATE_TIME = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z";
    private static final String BASIC_ZONED_DATE_TIME = "\\d{8}T\\d{6}Z";
    private static final String ISO_LOCAL_DATE = "\\d{4}-\\d{2}-\\d{2}";
    private static final String ISO_WEEK_DATE = "\\d{4}-W\\d{2}(-\\d)?";
    private static final String ISO_ORDINAL_DATE = "\\d{4}-\\d{3}";
    private static final String MONTH_DAY = "--\\d{2}-\\d{2}";

    /**
     * Parses an ISO 8601 formatted date string into a LocalDateTime object.
     *
     * @param isoDate the ISO8601 formatted date string.
     * @return the corresponding LocalDateTime object, or null if the input doesn't match the ISO8601 pattern.
     */
    public LocalDateTime parseIso8601(String isoDate) {
        LocalDateTime formattedTime;
        if (isoDate.matches(ISO_OFFSET_DATE_TIME)) {
            formattedTime = parseIsoOffsetDateTime(isoDate);
        } else if (isoDate.matches(ISO_ZONED_DATE_TIME)) {
            formattedTime = parseZonedDateTime(isoDate);
        } else if (isoDate.matches(BASIC_ZONED_DATE_TIME)) {
            formattedTime = parseBasicZonedDateTime(isoDate);
        } else if (isoDate.matches(ISO_LOCAL_DATE)) {
            formattedTime = parseLocalDate(isoDate);
        } else if (isoDate.matches(ISO_WEEK_DATE)) {
            formattedTime = parseIsoWeekDate(isoDate);
        } else if (isoDate.matches(ISO_ORDINAL_DATE)) {
            formattedTime = parseIsoOrdinalDate(isoDate);
        } else if (isoDate.matches(MONTH_DAY)) {
            formattedTime = parseMonthDay(isoDate);
        } else {
            return null;
        }
        return formattedTime;
    }

    private LocalDateTime parseIsoOffsetDateTime(String isoDate) {
        try {
            return OffsetDateTime.parse(isoDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDateTime parseZonedDateTime(String isoDate) {
        try {
            return ZonedDateTime.parse(isoDate, DateTimeFormatter.ISO_ZONED_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDateTime parseBasicZonedDateTime(String isoDate) {
        try {
            return ZonedDateTime.parse(isoDate, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDateTime parseLocalDate(String isoDate) {
        try {
            return LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDateTime parseIsoWeekDate(String isoDate) {
        try {
            // Make YYYY-Wxx-1 from YYYY-Wxx to specify the day (Monday)
            String isoDateCopy = isoDate;
            if (isoDate.matches("\\d{4}-W\\d{2}")) {
                isoDateCopy += "-1";
            }
            return LocalDate.parse(isoDateCopy, DateTimeFormatter.ISO_WEEK_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDateTime parseIsoOrdinalDate(String isoDate) {
        try {
            return LocalDate.parse(isoDate, DateTimeFormatter.ISO_ORDINAL_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDateTime parseMonthDay(String isoDate) {
        try {
            return MonthDay.parse(isoDate, DateTimeFormatter.ofPattern("--MM-dd")).atYear(Year.now().getValue())
                .atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
