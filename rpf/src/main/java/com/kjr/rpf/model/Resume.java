package com.kjr.rpf.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "resume_conversions")
@Data
public class Resume {

    @Id
    private String id;

    // Original document metadata
    private String originalFileName;
    private String originalFileType;
    private Long originalFileSize;
    private LocalDateTime uploadedAt;

    // Converted HTML content
    private String htmlContent;

    // Original binary data (for download)
    private byte[] originalFileData;

    // Parsed resume fields
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String linkedinUrl;
    private String websiteUrl;
    private String professionalSummary;
    private List<Education> education;
    private List<Experience> experience;
    private Skills skills;

    private String city;
    private String state;
    private String country;

    // Transient field for formatted file size (not stored in DB)
    private transient String formattedFileSize;

    // Getters and setters for formatted file size
    public String getFormattedFileSize() {
        return formattedFileSize;
    }

    public void setFormattedFileSize(String formattedFileSize) {
        this.formattedFileSize = formattedFileSize;
    }

}
