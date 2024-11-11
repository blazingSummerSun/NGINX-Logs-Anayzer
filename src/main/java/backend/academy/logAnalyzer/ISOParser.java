package backend.academy.logAnalyzer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ISOParser {

    public LocalDateTime parseIso8601(String isoDate) {
        LocalDateTime result = parseIsoOffsetDateTime(isoDate);
        if (result == null) {
            result = parseZonedDateTime(isoDate);
        }
        if (result == null) {
            result = parseBasicZonedDateTime(isoDate);
        }
        if (result == null) {
            result = parseLocalDate(isoDate);
        }
        if (result == null) {
            result = parseIsoWeekDate(isoDate);
        }
        if (result == null) {
            result = parseIsoWeekDateWithDay(isoDate);
        }
        if (result == null) {
            result = parseIsoOrdinalDate(isoDate);
        }
        if (result == null) {
            result = parseMonthDay(isoDate);
        }
        return result;
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
            String isoDateCopy = isoDate;
            if (isoDate.matches("\\d{4}-W\\d{2}")) {
                isoDateCopy += "-1";
            }
            return LocalDate.parse(isoDateCopy, DateTimeFormatter.ISO_WEEK_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDateTime parseIsoWeekDateWithDay(String isoDate) {
        try {
            return LocalDate.parse(isoDate, DateTimeFormatter.ISO_WEEK_DATE).atStartOfDay();
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
