package edu.csuf.sis.behavior;

import edu.csuf.sis.audit.GraduationCheck;
import edu.csuf.sis.catalog.ICatalog;
import edu.csuf.sis.catalog.TermView;
import edu.csuf.sis.identity.AccountView;
import edu.csuf.sis.identity.IUserAdmin;
import edu.csuf.sis.identity.NewProfessor;
import edu.csuf.sis.identity.NewStudent;
import edu.csuf.sis.records.IRecords;
import edu.csuf.sis.records.RosterEntry;
import edu.csuf.sis.records.Transcript;
import edu.csuf.sis.support.TestData;
import io.cucumber.spring.ScenarioScope;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.ScopedProxyMode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** What one scenario has set up so far: people by name, and the outcome of the last request. */
@TestComponent
@ScenarioScope(proxyMode = ScopedProxyMode.NO)
public class World {

    private final ICatalog catalog;
    private final IUserAdmin userAdmin;
    private final IRecords records;

    private final Map<String, String> cwids = new HashMap<>();
    private final Map<String, String> usernames = new HashMap<>();
    private final Map<String, Long> accountIds = new HashMap<>();
    private final Map<String, Long> professorIds = new HashMap<>();
    private final Map<String, String> tokens = new HashMap<>();

    private long registrarAccountId;
    private RuntimeException lastError;
    private GraduationCheck graduationCheck;
    private Transcript transcript;
    private List<RosterEntry> roll = List.of();
    private String reply;

    public World(ICatalog catalog, IUserAdmin userAdmin, IRecords records) {
        this.catalog = catalog;
        this.userAdmin = userAdmin;
        this.records = records;
    }

    /** Runs a request that may be refused and remembers the refusal for a later Then step. */
    public void attempt(Runnable request) {
        lastError = null;
        try {
            request.run();
        } catch (RuntimeException refusal) {
            lastError = refusal;
        }
    }

    public void addStudent(String name, String cwid, String majorCode) {
        if (cwids.containsKey(name)) {
            return;
        }
        String username = usernameFor(name);
        AccountView account = userAdmin.addStudent(new NewStudent(cwid, name, username + "@csu.fullerton.edu",
                majorCode, username, TestData.PASSWORD));
        cwids.put(name, cwid);
        usernames.put(name, username);
        accountIds.put(name, account.accountId());
    }

    /** The professor's id, adding the professor on first mention. */
    public long professorId(String name) {
        Long id = professorIds.get(name);
        if (id == null) {
            String username = usernameFor(name);
            AccountView account = userAdmin.addProfessor(new NewProfessor(name, "Computer Science",
                    username + "@fullerton.edu", username, TestData.PASSWORD));
            usernames.put(name, username);
            accountIds.put(name, account.accountId());
            id = userAdmin.professors().stream()
                    .filter(professor -> professor.name().equals(name))
                    .findFirst().orElseThrow().id();
            professorIds.put(name, id);
        }
        return id;
    }

    public String cwid(String name) {
        return required(cwids, name);
    }

    public long studentId(String name) {
        return records.studentByCwid(cwid(name)).id();
    }

    public String username(String name) {
        return required(usernames, name);
    }

    public long accountId(String name) {
        return required(accountIds, name);
    }

    public long termId(String termName) {
        return catalog.terms().stream()
                .filter(term -> term.name().equals(termName))
                .map(TermView::id)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No term called " + termName));
    }

    public long sectionId(String sectionNumber, String courseCode, String termName) {
        return catalog.sections(termId(termName), courseCode).stream()
                .filter(section -> section.courseCode().equals(courseCode)
                        && section.sectionNumber().equals(sectionNumber))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No section " + sectionNumber + " of " + courseCode + " in " + termName))
                .id();
    }

    public void rememberToken(String name, String token) {
        tokens.put(name, token);
    }

    public String token(String name) {
        return required(tokens, name);
    }

    public long registrarAccountId() {
        return registrarAccountId;
    }

    public void setRegistrarAccountId(long registrarAccountId) {
        this.registrarAccountId = registrarAccountId;
    }

    public RuntimeException lastError() {
        return lastError;
    }

    public GraduationCheck graduationCheck() {
        return graduationCheck;
    }

    public void setGraduationCheck(GraduationCheck graduationCheck) {
        this.graduationCheck = graduationCheck;
    }

    public Transcript transcript() {
        return transcript;
    }

    public void setTranscript(Transcript transcript) {
        this.transcript = transcript;
    }

    public List<RosterEntry> roll() {
        return roll;
    }

    public void setRoll(List<RosterEntry> roll) {
        this.roll = roll;
    }

    public String reply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    /** "Maria Lopez" becomes "mlopez". */
    public static String usernameFor(String fullName) {
        String[] parts = fullName.toLowerCase(Locale.ROOT).split("\\s+");
        return parts[0].charAt(0) + parts[parts.length - 1];
    }

    /** "CPSC 131, CPSC 362" becomes [CPSC 131, CPSC 362]. */
    public static List<String> list(String commaSeparated) {
        return Arrays.stream(commaSeparated.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private static <T> T required(Map<String, T> values, String name) {
        T value = values.get(name);
        if (value == null) {
            throw new IllegalStateException("Nobody called '" + name + "' was set up in this scenario");
        }
        return value;
    }
}
