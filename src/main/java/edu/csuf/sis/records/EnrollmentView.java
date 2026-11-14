package edu.csuf.sis.records;

import edu.csuf.sis.domain.EnrollmentStatus;
import edu.csuf.sis.domain.MeetingTime;

import java.time.LocalTime;

/** One class on a student's schedule. {@code grade} is null until a final grade is posted. */
public record EnrollmentView(long sectionId, long termId, String termName, String courseCode, String courseTitle,
                             int units, String sectionNumber, String days, LocalTime startTime, LocalTime endTime,
                             String meets, String room, String professorName, EnrollmentStatus status,
                             String grade) {

    public MeetingTime meetingTime() {
        return MeetingTime.of(days, startTime, endTime);
    }
}
