package edu.csuf.sis.common;

/** The requested record (student, course, section, ...) does not exist. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
