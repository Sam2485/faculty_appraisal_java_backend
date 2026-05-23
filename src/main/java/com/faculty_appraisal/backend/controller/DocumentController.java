package com.faculty_appraisal.backend.controller;

import com.faculty_appraisal.backend.model.entity.core.AppraisalDocument;
import com.faculty_appraisal.backend.repository.core.AppraisalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/appraisal-documents")
@RequiredArgsConstructor
public class DocumentController extends BaseController {

    private final AppraisalDocumentRepository documentRepo;

    @GetMapping
    public List<AppraisalDocument> getDocuments(
            @RequestParam("academic_year") String academicYear
    ) {
        return documentRepo.findByFacultyEmailAndAcademicYear(
                currentUser().getEmail(), academicYear);
    }
}
