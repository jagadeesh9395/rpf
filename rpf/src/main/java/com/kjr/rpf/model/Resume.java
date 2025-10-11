package com.kjr.rpf.model;

import com.kjr.rpf.config.DownloadConfig;
import lombok.Data;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Component;
import org.springframework.data.annotation.Id;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Document(collection = "resumes")
@Data
@Component
@Lazy
public class Resume {

    @Id
    private String id;
    
    private static DownloadConfig staticDownloadConfig;

    @org.springframework.beans.factory.annotation.Autowired
    public void setStaticDownloadConfig(DownloadConfig downloadConfig) {
        Resume.staticDownloadConfig = downloadConfig;
    }

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

    // Download tracking
    private Map<String, DownloadInfo> downloadHistory = new HashMap<>();
    private int downloadLimit = 3; // Default value, will be updated in @PostConstruct

    // Transient field for formatted file size (not stored in DB)
    private transient String formattedFileSize;

    // Getters and setters for formatted file size
    public String getFormattedFileSize() {
        return formattedFileSize;
    }

    public void setFormattedFileSize(String formattedFileSize) {
        this.formattedFileSize = formattedFileSize;
    }

    /**
     * Tracks a download by user session
     */
    @PostConstruct
    public void init() {
        if (staticDownloadConfig != null) {
            this.downloadLimit = staticDownloadConfig.getLimit();
        }
    }

    public synchronized boolean trackDownload(String sessionId) {
        // Check if download limits are enabled and enforce them if needed
        if (staticDownloadConfig != null && staticDownloadConfig.isLimitEnabled()) {
            if (downloadHistory.size() >= downloadLimit) {
                return false;
            }
        }

        // Track the download
        DownloadInfo info = new DownloadInfo();
        info.setDownloadedAt(LocalDateTime.now());
        downloadHistory.put(sessionId, info);
        return true;
    }

    /**
     * Checks if a session has reached download limit
     */
    public boolean hasReachedDownloadLimit(String sessionId) {
        // If download limits are disabled, always return false (no limit reached)
        if (staticDownloadConfig == null || !staticDownloadConfig.isLimitEnabled()) {
            return false;
        }

        // Check if this session has already downloaded this resume
        return downloadHistory.containsKey(sessionId);
    }

    /**
     * Gets masked email (shows first 3 characters and domain)
     */
    public String getMaskedEmail() {
        if (email == null || email.isEmpty()) {
            return "Not provided";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 3) {
            return "***" + email.substring(Math.max(0, atIndex));
        }

        return email.substring(0, 3) + "*****" + email.substring(atIndex);
    }

    /**
     * Gets masked phone number (shows last 4 digits)
     */
    public String getMaskedPhone() {
        if (phone == null || phone.isEmpty()) {
            return "Not provided";
        }

        if (phone.length() <= 4) {
            return "****";
        }

        return "*******" + phone.substring(phone.length() - 4);
    }

    @Data
    public static class DownloadInfo {
        private LocalDateTime downloadedAt;
        private String downloadId = UUID.randomUUID().toString();
    }

    /**
     * Returns the masked HTML content of the resume with minimal details
     *
     * @return masked HTML content with sensitive information redacted
     */
    public String getMaskedContent() {
        StringBuilder maskedContent = new StringBuilder();

        maskedContent.append("<div class='masked-resume'>");

        // Add minimal header
        maskedContent.append("<div class='resume-header'>");
        if (firstName != null || lastName != null) {
            maskedContent.append("<h1>");
            if (firstName != null) maskedContent.append(firstName).append(" ");
            if (lastName != null) maskedContent.append(lastName);
            maskedContent.append("</h1>");
        }

        // Add contact info with masked data
        maskedContent.append("<div class='contact-info'>");
        if (email != null && !email.isEmpty()) {
            maskedContent.append("<p><strong>Email:</strong> ").append(getMaskedEmail()).append("</p>");
        }
        if (phone != null && !phone.isEmpty()) {
            maskedContent.append("<p><strong>Phone:</strong> ").append(getMaskedPhone()).append("</p>");
        }
        if (city != null || state != null) {
            maskedContent.append("<p><strong>Location:</strong> ");
            if (city != null) maskedContent.append(city);
            if (city != null && state != null) maskedContent.append(", ");
            if (state != null) maskedContent.append(state);
            maskedContent.append("</p>");
        }
        maskedContent.append("</div>");

        // Add summary if available
        if (professionalSummary != null && !professionalSummary.trim().isEmpty()) {
            maskedContent.append("<div class='summary'>");
            maskedContent.append("<h3>Professional Summary</h3>");
            maskedContent.append("<p>" + professionalSummary.substring(0, Math.min(200, professionalSummary.length())) + "...");
            maskedContent.append(" <em>[Content truncated - login to view full details]</em></p>");
            maskedContent.append("</div>");
        }

        // Add skills section if available (just the categories, no details)
//        if (skills != null && !skills.getCategories().isEmpty()) {
//            maskedContent.append("<div class='skills'>");
//            maskedContent.append("<h3>Skills</h3>");
//            maskedContent.append("<p>" + skills.getCategories().size() + " skill categories available. Login to view details.</p>");
//            maskedContent.append("</div>");
//        }

        // Add experience summary
        if (experience != null && !experience.isEmpty()) {
            maskedContent.append("<div class='experience'>");
            maskedContent.append("<h3>Experience</h3>");
            maskedContent.append("<p>" + experience.size() + " positions available. Login to view details.</p>");
            maskedContent.append("</div>");
        }

        // Add education summary
        if (education != null && !education.isEmpty()) {
            maskedContent.append("<div class='education'>");
            maskedContent.append("<h3>Education</h3>");
            maskedContent.append("<p>" + education.size() + " education entries available. Login to view details.</p>");
            maskedContent.append("</div>");
        }

        maskedContent.append("</div>"); // Close resume-header
        maskedContent.append("</div>"); // Close masked-resume

        return maskedContent.toString();
    }

    /**
     * Returns the unmasked HTML content of the resume
     *
     * @return unmasked HTML content
     */
    public String getContent() {
        if (htmlContent == null) {
            return "<div>No content available</div>";
        }
        return htmlContent;
    }
}
