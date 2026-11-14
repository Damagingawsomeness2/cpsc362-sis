package edu.csuf.sis.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @EntityGraph(attributePaths = {"section", "section.course", "section.term", "section.professor"})
    List<Enrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    @EntityGraph(attributePaths = "student")
    List<Enrollment> findBySectionIdAndStatusInOrderByStudentNameAsc(Long sectionId,
                                                                     Collection<EnrollmentStatus> statuses);

    Optional<Enrollment> findByStudentIdAndSectionId(Long studentId, Long sectionId);
}
