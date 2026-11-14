package edu.csuf.sis.identity;

import java.time.Instant;

/** The bearer token is returned once, here; the server only keeps its hash. */
public record LoginResult(String token, Instant expiresAt, AuthenticatedUser user) {
}
