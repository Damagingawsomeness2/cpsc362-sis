package edu.csuf.sis.common;

/** The signed-in user may not act on this record, e.g. a professor grading another professor's section. */
public class PermissionDeniedException extends RuntimeException {

    public PermissionDeniedException(String message) {
        super(message);
    }
}
