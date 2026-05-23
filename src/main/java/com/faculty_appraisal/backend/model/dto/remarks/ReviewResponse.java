package com.faculty_appraisal.backend.model.dto.remarks;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResponse {
    private String message;
    private String status;
    private String decision;
    private String nextReviewer;
    private String nextReviewerRole;
    private String nextReviewerEmail;
}
