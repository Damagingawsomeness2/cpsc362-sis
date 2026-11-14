package edu.csuf.sis.identity;

import java.util.Optional;

/** Identity &amp; Access: sign-in. The API gateway checks every request's session token through this. */
public interface IAuth {

    /** @throws LoginFailedException for a wrong username/password or a revoked account */
    LoginResult login(String username, String password);

    /** The signed-in user for a session token, or empty if the token is unknown, expired or revoked. */
    Optional<AuthenticatedUser> authenticate(String token);

    void logout(String token);
}
