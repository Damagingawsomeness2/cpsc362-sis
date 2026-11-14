package edu.csuf.sis.identity;

import edu.csuf.sis.domain.Role;

/** Who is making a request. {@code studentId} or {@code professorId} is set for those roles. */
public record AuthenticatedUser(long accountId, String username, Role role, String displayName,
                                Long studentId, Long professorId) {
}
