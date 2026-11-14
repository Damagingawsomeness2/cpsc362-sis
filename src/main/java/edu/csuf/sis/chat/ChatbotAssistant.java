package edu.csuf.sis.chat;

import edu.csuf.sis.catalog.CourseView;
import edu.csuf.sis.catalog.ICatalog;
import edu.csuf.sis.catalog.SectionView;
import edu.csuf.sis.catalog.TermView;
import edu.csuf.sis.common.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A rule-based assistant: it recognizes a course code and a few kinds of question, and answers
 * from the catalog. Depends only on ICatalog (as in the component diagram), so it cannot see
 * anyone's grades. Swapping in an LLM later means writing another IChat implementation.
 */
@Service
public class ChatbotAssistant implements IChat {

    private static final Pattern COURSE_CODE = Pattern.compile("\\b([A-Za-z]{2,5})\\s?(\\d{3}[A-Za-z]?)\\b");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

    private static final String HELP = "I can answer questions about courses (descriptions, units, prerequisites), "
            + "open seats, and policies such as the add/drop deadline, grade levels and graduation checks. "
            + "Try asking: \"What are the prerequisites for CPSC 362?\"";
    private static final String GRADE_LEVELS = "Your grade level comes from the units you have passed: "
            + "freshman 0-29, sophomore 30-59, junior 60-89, senior 90 or more. "
            + "It updates automatically when final grades are posted.";
    private static final String GRADUATION = "To graduate you must pass every course your major requires and "
            + "complete the major's total units (120 for most bachelor's degrees). "
            + "The registrar's office runs the official graduation check.";
    private static final String REGISTRATION = "Open the Register tab, pick a term, search for a course and choose "
            + "a section with open seats. Registration checks prerequisites and time conflicts, and closes after "
            + "the first week of classes.";
    private static final String TRANSCRIPTS = "Official transcripts are produced by the registrar's office on request. "
            + "You can see your own courses and grades any time on the Academic History tab.";

    private final ICatalog catalog;

    public ChatbotAssistant(ICatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public ChatReply ask(String question) {
        String text = question == null ? "" : question.trim();
        String lower = text.toLowerCase(Locale.ROOT);

        Optional<String> courseCode = mentionedCourse(text);
        if (courseCode.isPresent()) {
            return new ChatReply(aboutCourse(courseCode.get(), lower));
        }
        if (mentions(lower, "drop", "add ", "withdraw", "deadline")) {
            return new ChatReply(addDropPolicy());
        }
        if (mentions(lower, "freshman", "sophomore", "junior", "senior", "grade level", "class level", "standing")) {
            return new ChatReply(GRADE_LEVELS);
        }
        if (mentions(lower, "graduat")) {
            return new ChatReply(GRADUATION);
        }
        if (mentions(lower, "register", "enroll", "sign up")) {
            return new ChatReply(REGISTRATION);
        }
        if (mentions(lower, "transcript")) {
            return new ChatReply(TRANSCRIPTS);
        }
        if (mentions(lower, "prereq")) {
            return new ChatReply("Which course? For example: \"What are the prerequisites for CPSC 362?\"");
        }
        return new ChatReply(HELP);
    }

    /** The first course code whose subject (CPSC, MATH, ...) is in the catalog, so "is 362" is ignored. */
    private Optional<String> mentionedCourse(String text) {
        Set<String> subjects = catalog.courses().stream()
                .map(course -> course.code().substring(0, course.code().indexOf(' ')))
                .collect(Collectors.toSet());
        Matcher matcher = COURSE_CODE.matcher(text);
        while (matcher.find()) {
            String subject = matcher.group(1).toUpperCase(Locale.ROOT);
            if (subjects.contains(subject)) {
                return Optional.of(subject + " " + matcher.group(2).toUpperCase(Locale.ROOT));
            }
        }
        return Optional.empty();
    }

    private String aboutCourse(String code, String question) {
        CourseView course;
        try {
            course = catalog.course(code);
        } catch (NotFoundException notInCatalog) {
            return "I couldn't find " + code + " in the catalog. Course codes look like CPSC 362.";
        }
        if (mentions(question, "seat", "open", "section", "available", "full", "space")) {
            return openSeats(course);
        }
        if (mentions(question, "prereq", "require", "need", "before")) {
            return prerequisites(course);
        }
        if (mentions(question, "drop", "deadline")) {
            return addDropPolicy();
        }
        return description(course);
    }

    private String prerequisites(CourseView course) {
        if (course.prerequisites().isEmpty()) {
            return course.code() + " (" + course.title() + ") has no prerequisites.";
        }
        String required = course.prerequisites().stream()
                .map(code -> code + " (" + catalog.course(code).title() + ")")
                .collect(Collectors.joining(" and "));
        return course.code() + " (" + course.title() + ") requires " + required
                + ". Each must be passed with a D- or better before you can register.";
    }

    private String openSeats(CourseView course) {
        List<String> lines = new ArrayList<>();
        for (TermView term : catalog.terms()) {
            if (!term.addDropOpen()) {
                continue;
            }
            for (SectionView section : catalog.sections(term.id(), course.code())) {
                if (section.courseCode().equals(course.code())) {
                    String seats = section.openSeats() == 0
                            ? "full (" + section.capacity() + (section.capacity() == 1 ? " seat)" : " seats)")
                            : section.openSeats() + " of " + section.capacity() + " seats open";
                    lines.add("- " + term.name() + " section " + section.sectionNumber() + " (" + section.meets()
                            + ", " + section.professorName() + "): " + seats);
                }
            }
        }
        if (lines.isEmpty()) {
            return "No sections of " + course.code() + " are open for registration right now.";
        }
        return "Sections of " + course.code() + " open for registration:\n" + String.join("\n", lines);
    }

    private String addDropPolicy() {
        String policy = "You can add or drop classes until the end of the first week of the term. "
                + "After that, students cannot drop (a professor can still drop a student from their class).";
        List<String> deadlines = catalog.terms().stream()
                .filter(TermView::addDropOpen)
                .map(term -> term.name() + ": the last day is " + DATE.format(term.addDropDeadline()))
                .toList();
        return deadlines.isEmpty() ? policy : policy + " " + String.join("; ", deadlines) + ".";
    }

    private static String description(CourseView course) {
        StringBuilder reply = new StringBuilder(course.code() + " - " + course.title() + " ("
                + course.units() + (course.units() == 1 ? " unit)." : " units)."));
        if (course.description() != null && !course.description().isBlank()) {
            reply.append(' ').append(course.description());
        }
        reply.append(course.prerequisites().isEmpty()
                ? " No prerequisites."
                : " Prerequisites: " + String.join(", ", course.prerequisites()) + ".");
        return reply.toString();
    }

    private static boolean mentions(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
