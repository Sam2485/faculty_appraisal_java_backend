package com.faculty_appraisal.backend.repository.core;

import com.faculty_appraisal.backend.model.entity.core.FacultyProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacultyProfileRepository extends JpaRepository<FacultyProfile, UUID> {

    Optional<FacultyProfile> findByEmail(String email);

    Optional<FacultyProfile> findByEmployeeId(String employeeId);

    List<FacultyProfile> findByDepartment(String department);

    List<FacultyProfile> findBySchool(String school);

    List<FacultyProfile> findByAppraisalRole(String appraisalRole);

    List<FacultyProfile> findByIsActiveTrue();

    List<FacultyProfile> findByIsVerifiedTrue();

    List<FacultyProfile> findByReportingOfficerEmail(String reportingOfficerEmail);

    List<FacultyProfile> findByRegistrarEmail(String registrarEmail);

    List<FacultyProfile> findBySchoolAndAppraisalRole(
            String school,
            String appraisalRole
    );

    Optional<FacultyProfile> findByEmailAndIsActiveTrue(String email);

    List<FacultyProfile> findBySchoolIn(Collection<String> schools);

    List<FacultyProfile> findBySchoolAndDepartment(String school, String department);

    List<FacultyProfile> findByAppraisalRoleIn(Collection<String> roles);

    List<FacultyProfile> findByReportingOfficerEmailAndReportsToRegistrarFalse(String reportingOfficerEmail);
}