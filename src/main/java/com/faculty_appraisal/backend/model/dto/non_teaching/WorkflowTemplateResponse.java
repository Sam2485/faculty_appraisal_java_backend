package com.faculty_appraisal.backend.model.dto.non_teaching;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class WorkflowTemplateResponse {
    private String workflowName;
    private List<StepDto> steps;

    @Data
    @AllArgsConstructor
    public static class StepDto {
        private int stepNo;
        private String designation;
        private String status;
    }
}
