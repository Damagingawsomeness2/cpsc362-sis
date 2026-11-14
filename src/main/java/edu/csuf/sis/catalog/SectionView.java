package edu.csuf.sis.catalog;

import edu.csuf.sis.domain.MeetingTime;

import java.time.LocalTime;

/** A section as shown when browsing the schedule: when it meets, who teaches it, and seats left. */
public record SectionView(long id, long termId, String termName, String courseCode, String courseTitle, int units,
                          String sectionNumber, Long professorId, String professorName,
                          String days, LocalTime startTime, LocalTime endTime, String meets, String room,
                          int capacity, int enrolled, int openSeats) {

    public MeetingTime meetingTime() {
        return MeetingTime.of(days, startTime, endTime);
    }
}
