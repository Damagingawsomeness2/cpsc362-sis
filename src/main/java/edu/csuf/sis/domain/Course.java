package edu.csuf.sis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A catalog course and the courses that must be passed before taking it (data store D2). */
@Entity
public class Course {

    private static final Pattern CODE_FORMAT = Pattern.compile("([A-Za-z]{2,5})\\s*(\\d{3}[A-Za-z]?)");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String code;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private int units;

    @ManyToMany
    @JoinTable(name = "course_prerequisite",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "prerequisite_id"))
    private Set<Course> prerequisites = new HashSet<>();

    protected Course() {
    }

    public Course(String code, String title, String description, int units) {
        if (units < 1 || units > 6) {
            throw new IllegalArgumentException("A course carries 1 to 6 units.");
        }
        this.code = normalizeCode(code);
        this.title = title;
        this.description = description;
        this.units = units;
    }

    /** Normalizes input such as "cpsc362" to the catalog form "CPSC 362". */
    public static String normalizeCode(String raw) {
        Matcher matcher = CODE_FORMAT.matcher(raw == null ? "" : raw.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("'" + raw + "' is not a course code like CPSC 362.");
        }
        return matcher.group(1).toUpperCase(Locale.ROOT) + " " + matcher.group(2).toUpperCase(Locale.ROOT);
    }

    public void addPrerequisite(Course prerequisite) {
        if (prerequisite == this || prerequisite.code.equals(code)) {
            throw new IllegalArgumentException(code + " cannot be its own prerequisite.");
        }
        prerequisites.add(prerequisite);
    }

    public List<String> prerequisiteCodes() {
        return prerequisites.stream().map(Course::getCode).sorted().toList();
    }

    /** Prerequisites not in {@code passedCourseCodes}, in code order. */
    public List<String> missingPrerequisites(Set<String> passedCourseCodes) {
        return prerequisiteCodes().stream().filter(code -> !passedCourseCodes.contains(code)).toList();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getUnits() {
        return units;
    }
}
