package edu.csuf.sis.gateway;

import edu.csuf.sis.identity.AccountView;
import edu.csuf.sis.identity.AuthenticatedUser;
import edu.csuf.sis.identity.IUserAdmin;
import edu.csuf.sis.identity.NewProfessor;
import edu.csuf.sis.identity.NewStudent;
import edu.csuf.sis.identity.ProfessorView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin portal: logins and passwords (Identity &amp; Access). */
@RestController
@RequestMapping("/api/admin")
public class AdminAccountController {

    private final IUserAdmin userAdmin;

    public AdminAccountController(IUserAdmin userAdmin) {
        this.userAdmin = userAdmin;
    }

    @GetMapping("/accounts")
    public List<AccountView> accounts() {
        return userAdmin.accounts();
    }

    @GetMapping("/professors")
    public List<ProfessorView> professors() {
        return userAdmin.professors();
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView addStudent(@Valid @RequestBody NewStudent student) {
        return userAdmin.addStudent(student);
    }

    @PostMapping("/professors")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView addProfessor(@Valid @RequestBody NewProfessor professor) {
        return userAdmin.addProfessor(professor);
    }

    @PostMapping("/accounts/{accountId}/revoke")
    public AccountView revokeAccess(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable long accountId) {
        return userAdmin.revokeAccess(accountId, admin.accountId());
    }

    @PostMapping("/accounts/{accountId}/restore")
    public AccountView restoreAccess(@PathVariable long accountId) {
        return userAdmin.restoreAccess(accountId);
    }

    @PutMapping("/accounts/{accountId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignPassword(@PathVariable long accountId, @Valid @RequestBody PasswordChange change) {
        userAdmin.assignPassword(accountId, change.password());
    }
}
