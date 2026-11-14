package edu.csuf.sis.identity;

import java.util.List;

/** Identity &amp; Access: the registrar's account management. */
public interface IUserAdmin {

    List<AccountView> accounts();

    List<ProfessorView> professors();

    AccountView addStudent(NewStudent student);

    AccountView addProfessor(NewProfessor professor);

    /** Disables the login and ends its sessions. The person's records and history are kept. */
    AccountView revokeAccess(long accountId, long requestedByAccountId);

    AccountView restoreAccess(long accountId);

    void assignPassword(long accountId, String newPassword);
}
