package edu.csuf.sis.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * When a section meets, e.g. "TuTh 1:00 PM–2:15 PM". Sections with no days are
 * "to be arranged" (online or independent study) and never conflict with anything.
 */
public record MeetingTime(Set<DayOfWeek> days, LocalTime start, LocalTime end) {

    public static final MeetingTime TBA = new MeetingTime(Set.of(), null, null);

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    // Two-letter codes are tried first so "TuTh" is read as Tu + Th.
    private static final Map<String, DayOfWeek> DAY_FOR_CODE = new LinkedHashMap<>();
    private static final Map<DayOfWeek, String> CODE_FOR_DAY = new LinkedHashMap<>();

    static {
        DAY_FOR_CODE.put("TU", DayOfWeek.TUESDAY);
        DAY_FOR_CODE.put("TH", DayOfWeek.THURSDAY);
        DAY_FOR_CODE.put("SA", DayOfWeek.SATURDAY);
        DAY_FOR_CODE.put("SU", DayOfWeek.SUNDAY);
        DAY_FOR_CODE.put("M", DayOfWeek.MONDAY);
        DAY_FOR_CODE.put("W", DayOfWeek.WEDNESDAY);
        DAY_FOR_CODE.put("F", DayOfWeek.FRIDAY);

        CODE_FOR_DAY.put(DayOfWeek.MONDAY, "M");
        CODE_FOR_DAY.put(DayOfWeek.TUESDAY, "Tu");
        CODE_FOR_DAY.put(DayOfWeek.WEDNESDAY, "W");
        CODE_FOR_DAY.put(DayOfWeek.THURSDAY, "Th");
        CODE_FOR_DAY.put(DayOfWeek.FRIDAY, "F");
        CODE_FOR_DAY.put(DayOfWeek.SATURDAY, "Sa");
        CODE_FOR_DAY.put(DayOfWeek.SUNDAY, "Su");
    }

    public MeetingTime {
        days = Set.copyOf(days);
        if (!days.isEmpty()) {
            if (start == null || end == null) {
                throw new IllegalArgumentException("A class that meets on set days needs a start and end time.");
            }
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("A class must end after it starts.");
            }
        }
    }

    /** Builds a meeting time from catalog day codes: M, Tu, W, Th, F, Sa, Su (e.g. "MW", "TuTh"). */
    public static MeetingTime of(String dayCodes, LocalTime start, LocalTime end) {
        if (dayCodes == null || dayCodes.isBlank()) {
            return TBA;
        }
        String codes = dayCodes.trim().toUpperCase(Locale.ROOT);
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        int position = 0;
        while (position < codes.length()) {
            int matchedLength = 0;
            for (Map.Entry<String, DayOfWeek> code : DAY_FOR_CODE.entrySet()) {
                if (codes.startsWith(code.getKey(), position)) {
                    days.add(code.getValue());
                    matchedLength = code.getKey().length();
                    break;
                }
            }
            if (matchedLength == 0) {
                throw new IllegalArgumentException(
                        "Unknown day in '" + dayCodes + "'. Use M, Tu, W, Th, F, Sa, Su (e.g. MW or TuTh).");
            }
            position += matchedLength;
        }
        return new MeetingTime(days, start, end);
    }

    public boolean isTba() {
        return days.isEmpty();
    }

    public boolean overlaps(MeetingTime other) {
        if (isTba() || other.isTba()) {
            return false;
        }
        boolean sharesADay = days.stream().anyMatch(other.days::contains);
        return sharesADay && start.isBefore(other.end) && other.start.isBefore(end);
    }

    /** Day codes in week order, e.g. "TuTh"; empty when to be arranged. */
    public String dayCodes() {
        StringBuilder codes = new StringBuilder();
        CODE_FOR_DAY.forEach((day, code) -> {
            if (days.contains(day)) {
                codes.append(code);
            }
        });
        return codes.toString();
    }

    public String describe() {
        if (isTba()) {
            return "TBA";
        }
        return dayCodes() + " " + CLOCK.format(start) + "–" + CLOCK.format(end);
    }
}
