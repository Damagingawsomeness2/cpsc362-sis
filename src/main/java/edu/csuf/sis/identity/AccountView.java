package edu.csuf.sis.identity;

import edu.csuf.sis.domain.Role;

/** A login as listed for the registrar. {@code cwid} is set for students, {@code department} for professors. */
public record AccountView(long accountId, String username, Role role, String displayName, boolean enabled,
                          String cwid, String department) {
}
