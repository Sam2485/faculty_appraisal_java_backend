package com.faculty_appraisal.backend.repository.core;

import com.faculty_appraisal.backend.model.entity.core.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {

    List<Announcement> findByIsActiveTrue();

    List<Announcement> findByIsActiveTrueOrderByCreatedAtDesc();

    List<Announcement> findAllByOrderByCreatedAtDesc();

    List<Announcement> findByAudience(String audience);

    List<Announcement> findByAudienceAndIsActiveTrue(String audience);

    List<Announcement> findByCreatedBy(String createdBy);

}