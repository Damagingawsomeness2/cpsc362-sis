package edu.csuf.sis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;

/** A semester, e.g. Fall 2026 (data store D3, Class Schedule). */
@Entity
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    protected Term() {
    }

    public Term(String name, LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("A term must end after it starts.");
        }
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /** Last day of the first week of classes. Students can add and drop through this day. */
    public LocalDate addDropDeadline() {
        return startDate.plusDays(6);
    }

    public boolean isAddDropOpen(LocalDate today) {
        return !today.isAfter(addDropDeadline());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
