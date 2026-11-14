package edu.csuf.sis.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @EntityGraph(attributePaths = {"major", "account"})
    Optional<Student> findByCwid(String cwid);

    @EntityGraph(attributePaths = {"major", "account"})
    Optional<Student> findWithMajorById(Long id);

    @EntityGraph(attributePaths = "account")
    List<Student> findAllByOrderByNameAsc();

    Optional<Student> findByAccountId(Long accountId);

    boolean existsByCwid(String cwid);
}
