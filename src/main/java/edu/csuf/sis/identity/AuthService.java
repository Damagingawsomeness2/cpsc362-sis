package edu.csuf.sis.identity;

import edu.csuf.sis.domain.Professor;
import edu.csuf.sis.domain.ProfessorRepository;
import edu.csuf.sis.domain.Role;
import edu.csuf.sis.domain.Student;
import edu.csuf.sis.domain.StudentRepository;
import edu.csuf.sis.domain.UserAccount;
import edu.csuf.sis.domain.UserAccountRepository;
import edu.csuf.sis.domain.UserSession;
import edu.csuf.sis.domain.UserSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@Transactional
public class AuthService implements IAuth {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserAccountRepository accounts;
    private final StudentRepository students;
    private final ProfessorRepository professors;
    private final UserSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Duration sessionLength;
    /** Checked when the username is unknown, so a wrong username takes as long as a wrong password. */
    private final String unknownUserHash;

    public AuthService(UserAccountRepository accounts, StudentRepository students, ProfessorRepository professors,
                       UserSessionRepository sessions, PasswordEncoder passwordEncoder, Clock clock,
                       @Value("${sis.auth.session-hours:8}") long sessionHours) {
        this.accounts = accounts;
        this.students = students;
        this.professors = professors;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.sessionLength = Duration.ofHours(sessionHours);
        this.unknownUserHash = passwordEncoder.encode("no such user");
    }

    @Override
    public LoginResult login(String username, String password) {
        Optional<UserAccount> account = accounts.findByUsernameIgnoreCase(username == null ? "" : username.trim());
        String storedHash = account.map(UserAccount::getPasswordHash).orElse(unknownUserHash);
        if (!passwordEncoder.matches(password == null ? "" : password, storedHash) || account.isEmpty()) {
            throw new LoginFailedException("Incorrect username or password.");
        }
        UserAccount user = account.get();
        if (!user.isEnabled()) {
            throw new LoginFailedException("Your access has been revoked. Please contact the registrar's office.");
        }

        Instant now = clock.instant();
        sessions.deleteExpired(now);
        String token = newToken();
        Instant expiresAt = now.plus(sessionLength);
        sessions.save(new UserSession(hash(token), user, expiresAt));
        return new LoginResult(token, expiresAt, authenticatedUser(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> authenticate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        return sessions.findByTokenHash(hash(token))
                .filter(session -> !session.isExpired(now))
                .map(UserSession::getAccount)
                .filter(UserAccount::isEnabled)
                .map(this::authenticatedUser);
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessions.findByTokenHash(hash(token)).ifPresent(sessions::delete);
        }
    }

    private AuthenticatedUser authenticatedUser(UserAccount account) {
        Long studentId = account.getRole() == Role.STUDENT
                ? students.findByAccountId(account.getId()).map(Student::getId).orElse(null)
                : null;
        Long professorId = account.getRole() == Role.PROFESSOR
                ? professors.findByAccountId(account.getId()).map(Professor::getId).orElse(null)
                : null;
        return new AuthenticatedUser(account.getId(), account.getUsername(), account.getRole(),
                account.getDisplayName(), studentId, professorId);
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is always available on the JVM", impossible);
        }
    }
}
