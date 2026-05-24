package com.faculty_appraisal.backend.model.entity.core;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appraisal_documents")
@Data
public class AppraisalDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "faculty_email", nullable = false)
    private String facultyEmail;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Column(name = "form_family")
    private String formFamily;

    @Column(nullable = false)
    private String section;

    @Column(name = "section_title")
    private String sectionTitle;

    @Column(name = "max_marks")
    private BigDecimal maxMarks;

    @Column(name = "row_no")
    private Integer rowNo;

    @Column(name = "doc_key")
    private String docKey;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
