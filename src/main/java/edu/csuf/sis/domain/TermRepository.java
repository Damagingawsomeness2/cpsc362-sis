package edu.csuf.sis.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {

    Optional<Term> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Term> findAllByOrderByStartDateAsc();
}
