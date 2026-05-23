package com.faculty_appraisal.backend.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class FeedbackRequest {

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 254, message = "Email must be 254 characters or less.")
    private String email;

    @NotBlank(message = "Category is required.")
    private String category;

    @NotBlank(message = "Subject is required.")
    @Size(max = 120, message = "Subject must be 120 characters or less.")
    private String subject;

    @NotBlank(message = "Message is required.")
    @Size(max = 5000, message = "Message must be 5000 characters or less.")
    private String message;

    @Size(max = 80, message = "Name must be 80 characters or less.")
    private String name;
}
