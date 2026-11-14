package edu.csuf.sis.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {

    Optional<Professor> findByAccountId(Long accountId);

    @EntityGraph(attributePaths = "account")
    List<Professor> findAllByOrderByNameAsc();
}
