package edu.csuf.sis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A degree program and the courses it requires (data store D5, Degree Requirements). */
@Entity
public class Major {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String degree;

    @Column(nullable = false)
    private int totalUnits;

    @ManyToMany
    @JoinTable(name = "major_required_course",
            joinColumns = @JoinColumn(name = "major_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id"))
    private Set<Course> requiredCourses = new HashSet<>();

    protected Major() {
    }

    public Major(String code, String name, String degree, int totalUnits, Collection<Course> requiredCourses) {
        this.code = code;
        update(name, degree, totalUnits, requiredCourses);
    }

    public void update(String name, String degree, int totalUnits, Collection<Course> requiredCourses) {
        this.name = name;
        this.degree = degree;
        this.totalUnits = totalUnits;
        this.requiredCourses.clear();
        this.requiredCourses.addAll(requiredCourses);
    }

    public List<String> requiredCourseCodes() {
        return requiredCourses.stream().map(Course::getCode).sorted().toList();
    }

    /** The graduation check: every required course passed and enough total units. */
    public DegreeProgress evaluate(Set<String> passedCourseCodes, int unitsCompleted) {
        List<String> required = requiredCourseCodes();
        List<String> completed = required.stream().filter(passedCourseCodes::contains).toList();
        List<String> missing = required.stream().filter(code -> !passedCourseCodes.contains(code)).toList();
        return new DegreeProgress(completed, missing, unitsCompleted, totalUnits);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDegree() {
        return degree;
    }

    public int getTotalUnits() {
        return totalUnits;
    }
}
