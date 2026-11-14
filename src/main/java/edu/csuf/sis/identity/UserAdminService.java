package edu.csuf.sis.identity;

import edu.csuf.sis.common.NotFoundException;
import edu.csuf.sis.common.RuleViolationException;
import edu.csuf.sis.domain.Major;
import edu.csuf.sis.domain.MajorRepository;
import edu.csuf.sis.domain.Professor;
import edu.csuf.sis.domain.ProfessorRepository;
import edu.csuf.sis.domain.Role;
import edu.csuf.sis.domain.Student;
import edu.csuf.sis.domain.StudentRepository;
import edu.csuf.sis.domain.UserAccount;
import edu.csuf.sis.domain.UserAccountRepository;
import edu.csuf.sis.domain.UserSessionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserAdminService implements IUserAdmin {

    private final UserAccountRepository accounts;
    private final StudentRepository students;
    private final ProfessorRepository professors;
    private final MajorRepository majors;
    private final UserSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(UserAccountRepository accounts, StudentRepository students,
                            ProfessorRepository professors, MajorRepository majors,
                            UserSessionRepository sessions, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.students = students;
        this.professors = professors;
        this.majors = majors;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountView> accounts() {
        Map<Long, Student> studentByAccount = students.findAllByOrderByNameAsc().stream()
                .collect(Collectors.toMap(student -> student.getAccount().getId(), Function.identity()));
        Map<Long, Professor> professorByAccount = professors.findAllByOrderByNameAsc().stream()
                .collect(Collectors.toMap(professor -> professor.getAccount().getId(), Function.identity()));
        return accounts.findAllByOrderByRoleAscDisplayNameAsc().stream()
                .map(account -> view(account, studentByAccount.get(account.getId()),
                        professorByAccount.get(account.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessorView> professors() {
        return professors.findAllByOrderByNameAsc().stream()
                .map(professor -> new ProfessorView(professor.getId(), professor.getName(),
                        professor.getDepartment(), professor.getEmail()))
                .toList();
    }

    @Override
    public AccountView addStudent(NewStudent request) {
        String cwid = request.cwid().trim();
        if (students.existsByCwid(cwid)) {
            throw new RuleViolationException("A student with CWID " + cwid + " already exists.");
        }
        Major major = null;
        if (request.majorCode() != null && !request.majorCode().isBlank()) {
            String majorCode = request.majorCode().trim().toUpperCase(Locale.ROOT);
            major = majors.findByCode(majorCode)
                    .orElseThrow(() -> new RuleViolationException("There is no major with code " + majorCode + "."));
        }
        UserAccount account = newAccount(request.username(), request.password(), Role.STUDENT, request.name());
        Student student = students.save(new Student(cwid, request.name().trim(), blankToNull(request.email()),
                major, account));
        return view(account, student, null);
    }

    @Override
    public AccountView addProfessor(NewProfessor request) {
        UserAccount account = newAccount(request.username(), request.password(), Role.PROFESSOR, request.name());
        Professor professor = professors.save(new Professor(request.name().trim(), request.department().trim(),
                blankToNull(request.email()), account));
        return view(account, null, professor);
    }

    @Override
    public AccountView revokeAccess(long accountId, long requestedByAccountId) {
        if (accountId == requestedByAccountId) {
            throw new RuleViolationException("You cannot revoke your own access.");
        }
        UserAccount account = findAccount(accountId);
        account.revokeAccess();
        sessions.deleteAllForAccount(accountId);
        return view(account);
    }

    @Override
    public AccountView restoreAccess(long accountId) {
        UserAccount account = findAccount(accountId);
        account.restoreAccess();
        return view(account);
    }

    @Override
    public void assignPassword(long accountId, String newPassword) {
        checkPassword(newPassword);
        findAccount(accountId).changePasswordHash(passwordEncoder.encode(newPassword));
        sessions.deleteAllForAccount(accountId);
    }

    private UserAccount newAccount(String username, String password, Role role, String displayName) {
        String login = username.trim();
        if (accounts.existsByUsernameIgnoreCase(login)) {
            throw new RuleViolationException("The username " + login + " is already taken.");
        }
        checkPassword(password);
        return accounts.save(new UserAccount(login, passwordEncoder.encode(password), role, displayName.trim()));
    }

    private static void checkPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new RuleViolationException("Passwords must be at least 8 characters.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RuleViolationException("Passwords can be at most 72 characters.");
        }
    }

    private UserAccount findAccount(long accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> new NotFoundException("There is no account with id " + accountId + "."));
    }

    private AccountView view(UserAccount account) {
        Student student = account.getRole() == Role.STUDENT
                ? students.findByAccountId(account.getId()).orElse(null) : null;
        Professor professor = account.getRole() == Role.PROFESSOR
                ? professors.findByAccountId(account.getId()).orElse(null) : null;
        return view(account, student, professor);
    }

    private static AccountView view(UserAccount account, Student student, Professor professor) {
        return new AccountView(account.getId(), account.getUsername(), account.getRole(), account.getDisplayName(),
                account.isEnabled(), student == null ? null : student.getCwid(),
                professor == null ? null : professor.getDepartment());
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
