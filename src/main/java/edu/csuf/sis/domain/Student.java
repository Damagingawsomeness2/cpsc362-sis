package edu.csuf.sis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

/** A student (data store D1). Units completed and grade level are kept current as grades are posted. */
@Entity
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Campus-wide ID, e.g. 885104273. */
    @Column(nullable = false, unique = true, length = 9)
    private String cwid;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 120)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id")
    private Major major;

    @Column(nullable = false)
    private int unitsCompleted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GradeLevel gradeLevel = GradeLevel.FRESHMAN;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private UserAccount account;

    protected Student() {
    }

    public Student(String cwid, String name, String email, Major major, UserAccount account) {
        this.cwid = cwid;
        this.name = name;
        this.email = email;
        this.major = major;
        this.account = account;
    }

    /** Records the units passed so far and the class standing they earn. */
    public void updateStanding(int unitsCompleted) {
        this.unitsCompleted = unitsCompleted;
        this.gradeLevel = GradeLevel.forUnits(unitsCompleted);
    }

    public Long getId() {
        return id;
    }

    public String getCwid() {
        return cwid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Major getMajor() {
        return major;
    }

    public int getUnitsCompleted() {
        return unitsCompleted;
    }

    public GradeLevel getGradeLevel() {
        return gradeLevel;
    }

    public UserAccount getAccount() {
        return account;
    }
}
