package edu.csuf.sis.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {

    @EntityGraph(attributePaths = {"course", "term", "professor"})
    List<Section> findByTermIdOrderByCourseCodeAscSectionNumberAsc(Long termId);

    @EntityGraph(attributePaths = {"course", "term", "professor"})
    List<Section> findByProfessorIdOrderByTermStartDateDescCourseCodeAsc(Long professorId);

    @EntityGraph(attributePaths = {"course", "term", "professor"})
    Optional<Section> findWithDetailsById(Long id);

    /** Locks the row so two students cannot both take the last seat. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Section s where s.id = :id")
    Optional<Section> findForUpdate(Long id);

    boolean existsByCourseIdAndTermIdAndSectionNumber(Long courseId, Long termId, String sectionNumber);

    Optional<Section> findByCourseCodeAndTermNameAndSectionNumber(String courseCode, String termName,
                                                                  String sectionNumber);
}
