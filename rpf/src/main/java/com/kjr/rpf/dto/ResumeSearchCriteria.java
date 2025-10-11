package com.kjr.rpf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeSearchCriteria {

    // Name search
    private String firstName;
    private String lastName;
    private String fullName; // Search in both first and last name

    // Contact search
    private String email;
    private String phone;

    // Location search
    private String city;
    private String state;

    // Skills search
    @Builder.Default
    private List<String> programmingLanguages = new ArrayList<>();
    @Builder.Default
    private List<String> frameworks = new ArrayList<>();
    @Builder.Default
    private List<String> databases = new ArrayList<>();
    @Builder.Default
    private List<String> tools = new ArrayList<>();
    @Builder.Default
    private List<String> cloudTechnologies = new ArrayList<>();
    private String anySkill; // Search across all skill categories

    // Experience search
    private String companyName;
    private String jobTitle;
    private Integer minYearsOfExperience;

    // Education search
    private String degree;
    private String institution;
    private String major;
    private Double minGpa;

    // General search
    private String keyword; // Search across all text fields

    // Date range
    private String uploadedAfter; // ISO date string
    private String uploadedBefore; // ISO date string
}
