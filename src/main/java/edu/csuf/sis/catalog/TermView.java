package edu.csuf.sis.catalog;

import java.time.LocalDate;

/** A term plus whether students can still add or drop classes in it today. */
public record TermView(long id, String name, LocalDate startDate, LocalDate endDate,
                       LocalDate addDropDeadline, boolean addDropOpen) {
}
