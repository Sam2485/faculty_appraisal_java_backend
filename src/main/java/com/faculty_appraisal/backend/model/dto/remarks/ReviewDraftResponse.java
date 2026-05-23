package com.faculty_appraisal.backend.model.dto.remarks;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewDraftResponse {
    private Map<String, Object> payload;
    private LocalDateTime updatedAt;
}
