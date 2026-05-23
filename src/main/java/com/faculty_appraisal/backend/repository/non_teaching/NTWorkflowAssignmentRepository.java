package com.faculty_appraisal.backend.repository.non_teaching;

import com.faculty_appraisal.backend.model.entity.non_teaching.NTWorkflowAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NTWorkflowAssignmentRepository extends JpaRepository<NTWorkflowAssignment, UUID> {

    @EntityGraph(attributePaths = {"template", "template.steps", "template.steps.designation"})
    Optional<NTWorkflowAssignment> findByStaffEmail(String staffEmail);

    @EntityGraph(attributePaths = {"template", "template.steps", "template.steps.designation"})
    Optional<NTWorkflowAssignment> findByDepartment(String department);

    @EntityGraph(attributePaths = {"template", "template.steps", "template.steps.designation"})
    Optional<NTWorkflowAssignment> findByAppraisalRole(String appraisalRole);

    List<NTWorkflowAssignment> findAllByOrderByCreatedAtDesc();
}
