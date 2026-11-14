package edu.csuf.sis.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @EntityGraph(attributePaths = "prerequisites")
    Optional<Course> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "prerequisites")
    List<Course> findAllByOrderByCodeAsc();
}
