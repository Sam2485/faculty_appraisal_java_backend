package com.faculty_appraisal.backend.model.dto.non_teaching;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowStatusResponse {
    private String workflowId;
    private String workflowName;
    private Integer currentStep;
    private String status;
    private List<StepStatusDto> steps;

    @Data
    public static class StepStatusDto {
        private int stepNo;
        private String designation;
        private String status;
    }
}
