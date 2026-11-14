package edu.csuf.sis.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MajorRepository extends JpaRepository<Major, Long> {

    @EntityGraph(attributePaths = "requiredCourses")
    Optional<Major> findByCode(String code);

    @EntityGraph(attributePaths = "requiredCourses")
    List<Major> findAllByOrderByCodeAsc();
}
