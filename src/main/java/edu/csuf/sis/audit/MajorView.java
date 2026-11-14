package edu.csuf.sis.audit;

import java.util.List;

public record MajorView(String code, String name, String degree, int totalUnits, List<String> requiredCourses) {
}
