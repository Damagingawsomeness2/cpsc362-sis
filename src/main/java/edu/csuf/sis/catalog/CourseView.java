package edu.csuf.sis.catalog;

import java.util.List;

public record CourseView(String code, String title, String description, int units, List<String> prerequisites) {
}
