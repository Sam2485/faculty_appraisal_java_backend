package com.faculty_appraisal.backend.repository.non_teaching;

import com.faculty_appraisal.backend.model.entity.non_teaching.NTWorkflowTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NTWorkflowTemplateRepository extends JpaRepository<NTWorkflowTemplate, UUID> {

    @EntityGraph(attributePaths = {"steps", "steps.designation"})
    Optional<NTWorkflowTemplate> findFirstByIsDefaultTrueAndIsActiveTrue();

    @EntityGraph(attributePaths = {"steps", "steps.designation"})
    @Query("SELECT t FROM NTWorkflowTemplate t WHERE t.id = :id")
    Optional<NTWorkflowTemplate> findByIdWithSteps(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"steps", "steps.designation"})
    @Query("SELECT t FROM NTWorkflowTemplate t ORDER BY t.isDefault DESC, t.name ASC")
    List<NTWorkflowTemplate> findAllWithStepsOrdered();
}
