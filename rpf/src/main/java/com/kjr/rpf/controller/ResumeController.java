package com.kjr.rpf.controller;

import com.kjr.rpf.dto.ResumeSearchCriteria;
import com.kjr.rpf.model.Resume;
import com.kjr.rpf.service.ResumeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/resumes")
@Slf4j
public class ResumeController {

    private final ResumeService resumeService;


    @Autowired
    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    /**
     * Show all resumes page
     */
    /**
     * Delete a resume by ID (ADMIN only)
     */
    @PostMapping("/delete/{id}")
    public String deleteResume(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            resumeService.deleteResume(id);
            redirectAttributes.addFlashAttribute("successMessage", "Resume deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting resume: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting resume: " + e.getMessage());
        }
        return "redirect:/resumes/list";
    }

    @GetMapping("/list")
    public String showAllResumes(Model model) {
        List<Resume> resumes = resumeService.getAllResumes();

        // Format file sizes and set formatted file size for each resume
        for (Resume resume : resumes) {
            if (resume.getOriginalFileSize() != null) {
                String formattedSize = formatFileSize(resume.getOriginalFileSize());
                resume.setFormattedFileSize(formattedSize);
            } else {
                resume.setFormattedFileSize("0 KB");
            }

            // Ensure uploadedAt is set to current time if null
            if (resume.getUploadedAt() == null) {
                resume.setUploadedAt(LocalDateTime.now());
            }
        }

        model.addAttribute("resumes", resumes);
        return "list";
    }

    /**
     * Debug endpoint to test search functionality
     */
    @GetMapping("/debug/search")
    @ResponseBody
    public ResponseEntity<String> debugSearch() {
        try {
            List<Resume> allResumes = resumeService.getAllResumes();
            return ResponseEntity.ok("Database contains " + allResumes.size() + " resumes");
        } catch (Exception e) {
            return ResponseEntity.ok("Error accessing database: " + e.getMessage());
        }
    }

    /**
     * Show upload page
     */
    @GetMapping("/upload")
    public String showUploadPage() {
        return "upload";
    }

    /**
     * Upload resume document
     */
    @PostMapping("/upload")
    public String uploadResume(@RequestParam("file") MultipartFile file,
                             @RequestParam("firstName") String firstName,
                             @RequestParam("lastName") String lastName,
                             HttpSession session,
                             Model model) {
        try {
            if (file.isEmpty()) {
                model.addAttribute("error", "Please select a file to upload");
                return "upload";
            }
            
            // Validate first name and last name
            if (firstName == null || firstName.trim().isEmpty()) {
                model.addAttribute("error", "Please enter first name");
                return "upload";
            }
            
            if (lastName == null || lastName.trim().isEmpty()) {
                model.addAttribute("error", "Please enter last name");
                return "upload";
            }
            
            // Clean the names
            firstName = firstName.trim();
            lastName = lastName.trim();

            Resume resume = resumeService.uploadAndConvertResume(file, firstName, lastName);

            // Store the uploaded resume ID in session for preview
            session.setAttribute("previewResumeId", resume.getId());

            // Redirect to view with preview flag
            return "redirect:/resumes/view/" + resume.getId() + "?preview=true";

        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            model.addAttribute("error", "Error uploading file: " + e.getMessage());
            return "upload";
        }
    }

    /**
     * Check if current user is authenticated
     */
    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() &&
                !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"));
    }

    /**
     * View resume by ID with search context
     */
    @GetMapping("/view/{id}")
    public String viewResume(@PathVariable String id,
                             @RequestParam(required = false) String preview,
                             @RequestParam(required = false) String searchQuery,
                             @RequestParam(required = false) String searchContext,
                             HttpSession session,
                             Model model) {
        Optional<Resume> resumeOpt = resumeService.getResumeById(id);
        if (resumeOpt.isEmpty()) {
            return "redirect:/resumes/list?error=Resume+not+found";
        }

        Resume resume = resumeOpt.get();
        model.addAttribute("resume", resume);
        model.addAttribute("resumeId", resume.getId());
        model.addAttribute("originalFileName", resume.getOriginalFileName());
        model.addAttribute("fileType", resume.getOriginalFileType());
        model.addAttribute("fileSize", formatFileSize(resume.getOriginalFileSize()));
        model.addAttribute("uploadDate", resume.getUploadedAt());
        model.addAttribute("isAuthenticated", isAuthenticated());

        // Check if this is a preview (from upload)
        boolean isPreview = "true".equalsIgnoreCase(preview);
        model.addAttribute("isPreview", isPreview);

        // Check if this is from search results
        boolean isFromSearch = "true".equalsIgnoreCase(searchContext) || searchQuery != null;
        model.addAttribute("isFromSearch", isFromSearch);
        model.addAttribute("searchQuery", searchQuery);

        // For authenticated users, show full content by default
        // For unauthenticated users or explicit mask request, show masked content
        boolean masked = !isAuthenticated() && !isPreview;
        model.addAttribute("masked", masked);

        // Get the appropriate content based on authentication and masking preference
        String content = null;
        if (masked) {
            // For masked view, get masked content
            content = resumeService.getResumeHtmlContent(id, true);
        } else {
            // For authenticated users, get unmasked content
            content = resumeService.getResumeHtmlContent(id, false);
        }

        // Add content to model - for template compatibility
        model.addAttribute("content", content);
        model.addAttribute("maskedContent", content); // For backward compatibility

        // If this is a preview, store in session to allow download
        if (isPreview) {
            session.setAttribute("previewResumeId", id);
        }

        return "viewer";
    }

    /**
     * View unmasked resume (requires authentication)
     */
    @GetMapping("/unmasked/{id}")
    public ResponseEntity<String> viewUnmaskedResume(@PathVariable String id) {
        if (!isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String htmlContent = resumeService.getResumeHtmlContent(id, false);
        if (htmlContent == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(htmlContent);
    }

    /**
     * Download original resume file (requires authentication)
     */
    @GetMapping("/download/{id}")
    public String downloadResume(@PathVariable String id,
                                 @RequestParam(required = false) String token,
                                 @RequestParam(required = false) String download,
                                 HttpSession session,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 RedirectAttributes redirectAttributes) throws IOException {

        // Check if this is a preview download (from upload page)
        String previewResumeId = (String) session.getAttribute("previewResumeId");
        boolean isPreview = id.equals(previewResumeId);

        // For non-preview downloads, check authentication
        if (!isPreview && !isAuthenticated()) {
            String referer = request.getHeader("referer");
            String redirectUrl = referer != null ? referer : "/resumes/view/" + id;
            session.setAttribute("loginRedirect", redirectUrl);
            return "redirect:/login?require_auth=1";
        }

        try {
            Resume resume = resumeService.getResumeById(id)
                    .orElseThrow(() -> new RuntimeException("Resume not found with id: " + id));

            // If this is the initial download request (not the actual file download)
            if (download == null) {
                // For non-preview downloads, check download limits
                if (!isPreview) {
                    Integer downloadCount = (Integer) session.getAttribute("downloadCount");
                    if (downloadCount == null) {
                        downloadCount = 0;
                    }

                    final int MAX_DOWNLOADS_PER_SESSION = 3;
                    if (downloadCount >= MAX_DOWNLOADS_PER_SESSION) {
                        return "redirect:/resumes/download-limit-reached";
                    }

                    // Generate a new download token
                    String downloadToken = UUID.randomUUID().toString();
                    session.setAttribute("downloadToken", downloadToken);

                    // Redirect to thank you page with the token
                    redirectAttributes.addFlashAttribute("resumeId", id);
                    redirectAttributes.addFlashAttribute("resumeName", resume.getOriginalFileName());
                    redirectAttributes.addFlashAttribute("downloadToken", downloadToken);
                    redirectAttributes.addFlashAttribute("downloadsRemaining", MAX_DOWNLOADS_PER_SESSION - downloadCount - 1);

                    return "redirect:/resumes/thank-you";
                }

                // For previews, go directly to download
                String downloadToken = "preview_" + UUID.randomUUID().toString();
                session.setAttribute("downloadToken", downloadToken);
                return "redirect:/resumes/download/" + id + "?token=" + downloadToken + "&download=true";
            }

            // This is the actual file download request
            if (Boolean.TRUE.toString().equals(download) || "true".equals(download)) {
                // Verify download token for non-preview downloads
                if (!isPreview) {
                    String sessionToken = (String) session.getAttribute("downloadToken");
                    if (sessionToken == null || !sessionToken.equals(token)) {
                        return "redirect:/resumes/view/" + id + "?error=Invalid+download+token";
                    }

                    // Increment download count
                    Integer downloadCount = (Integer) session.getAttribute("downloadCount");
                    if (downloadCount == null) {
                        downloadCount = 0;
                    }
                    session.setAttribute("downloadCount", downloadCount + 1);
                }

                // Set up file download headers
                response.setContentType(resume.getOriginalFileType());
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"" + resume.getOriginalFileName() + "\"");
                response.setContentLength(resume.getOriginalFileData().length);

                // Write file to response
                try (OutputStream out = response.getOutputStream()) {
                    out.write(resume.getOriginalFileData());
                }

                return null;
            }

            // If we get here, it's an invalid request
            return "redirect:/resumes/view/" + id;

        } catch (Exception e) {
            log.error("Error downloading resume", e);
            redirectAttributes.addFlashAttribute("error", "Error downloading resume: " + e.getMessage());
            return "redirect:/resumes/view/" + id;
        }
    }

    /**
     * Show thank you page for downloads
     */
    @GetMapping("/thank-you")
    public String showThankYouPage(Model model, HttpSession session) {
        // Check if we have the required attributes in the flash attributes
        Map<String, ?> flashAttributes = RequestContextUtils.getInputFlashMap(
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());

        if (flashAttributes == null || !flashAttributes.containsKey("resumeId")) {
            return "redirect:/resumes/list";
        }

        // Add all flash attributes to the model
        model.addAllAttributes(flashAttributes);

        // Add session-specific attributes
        Integer downloadCount = (Integer) session.getAttribute("downloadCount");
        if (downloadCount != null) {
            model.addAttribute("downloadsRemaining", 3 - downloadCount);
        } else {
            model.addAttribute("downloadsRemaining", 3);
        }

        return "download-thank-you";
    }

    /**
     * Serve the actual file for download
     */
    private ResponseEntity<ByteArrayResource> serveFile(Resume resume) {
        byte[] fileData = resume.getOriginalFileData();
        String fileName = resume.getOriginalFileName() != null ?
                resume.getOriginalFileName() : "resume_" + resume.getId() + ".pdf";

        ByteArrayResource resource = new ByteArrayResource(fileData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(resume.getOriginalFileType()))
                .contentLength(fileData.length)
                .body(resource);
    }

    /**
     * Get all resumes (API endpoint)
     */
    /**
     * Get resume content with optional masking
     */
    @GetMapping("/view/{id}/content")
    @ResponseBody
    public ResponseEntity<String> getResumeContent(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "true") boolean masked) {
        log.info("Fetching resume content for ID: {}, masked: {}", id, masked);
        try {
            // Input validation
            if (id == null || id.trim().isEmpty()) {
                log.warn("Empty resume ID provided");
                return ResponseEntity.badRequest().body("Resume ID cannot be empty");
            }

            // Check authentication for unmasked content
            if (!masked && !isAuthenticated()) {
                log.warn("Unauthenticated user requested unmasked content for resume: {}", id);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Authentication required for unmasked content");
            }

            // Get resume from service
            return resumeService.getResumeById(id)
                    .map(resume -> {
                        try {
                            String content = resumeService.getResumeHtmlContent(id, masked);
                            if (content == null || content.trim().isEmpty()) {
                                log.error("Empty content returned for resume: {}", id);
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body("Resume content is empty");
                            }
                            log.debug("Successfully retrieved content for resume: {}, masked: {}", id, masked);
                            return ResponseEntity.ok(content);
                        } catch (Exception e) {
                            log.error("Error processing resume content for ID: {}", id, e);
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body("Error processing resume content");
                        }
                    })
                    .orElseGet(() -> {
                        log.warn("Resume not found with ID: {}", id);
                        return ResponseEntity.notFound().build();
                    });

        } catch (Exception e) {
            log.error("Unexpected error in getResumeContent for ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }

    @GetMapping("/api/all")
    @ResponseBody
    public ResponseEntity<?> getAllResumes() {
        try {
            List<Resume> resumes = resumeService.getAllResumes();

            // Format file sizes and mask sensitive data for each resume
            resumes.forEach(resume -> {
                if (resume.getOriginalFileSize() != null) {
                    resume.setFormattedFileSize(formatFileSize(resume.getOriginalFileSize()));
                }
                // Mask sensitive data
                if (!isAuthenticated()) {
                    resume.setEmail(resume.getMaskedEmail());
                    resume.setPhone(resume.getMaskedPhone());
                }
            });

            return ResponseEntity.ok(resumes);
        } catch (Exception e) {
            log.error("Error retrieving all resumes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving resumes");
        }
    }

    /**
     * Get resume by ID (API endpoint)
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> getResumeById(@PathVariable String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Resume ID cannot be empty");
            }

            return resumeService.getResumeById(id)
                    .map(resume -> {
                        // Format file size
                        if (resume.getOriginalFileSize() != null) {
                            resume.setFormattedFileSize(formatFileSize(resume.getOriginalFileSize()));
                        }

                        // Mask sensitive data for unauthenticated users
                        if (!isAuthenticated()) {
                            resume.setEmail(resume.getMaskedEmail());
                            resume.setPhone(resume.getMaskedPhone());
                        }

                        return ResponseEntity.ok(resume);
                    })
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("Error retrieving resume with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving resume");
        }
    }

    /**
     * Show download limit reached page
     */
    @GetMapping("/download-limit-reached")
    public String showDownloadLimitReached() {
        return "download-limit-reached";
    }

    /**
     * Handle download completion (AJAX endpoint)
     */
    @PostMapping("/api/download-complete")
    @ResponseBody
    public ResponseEntity<String> completeDownload(@RequestBody Map<String, String> payload,
                                                   HttpSession session) {
        String token = payload.get("token");
        String sessionToken = (String) session.getAttribute("downloadToken");

        if (token != null && token.equals(sessionToken)) {
            // Clear the token after successful download
            session.removeAttribute("downloadToken");
            return ResponseEntity.ok("Download completed");
        }
        return ResponseEntity.badRequest().body("Invalid or expired token");
    }

    /**
     * Show search page
     */
    @GetMapping("/search")
    public String showSearchPage(Model model) {
        return "search";
    }

    /**
     * Search resumes (POST request with form data)
     */
    @PostMapping("/search")
    public String searchResumes(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String uploadedBefore,
            Model model) {

        // Create search criteria with the query as the keyword
        // This will search across all text fields including name, skills, location, etc.
        ResumeSearchCriteria criteria = ResumeSearchCriteria.builder()
                .keyword(query)
                .uploadedBefore(uploadedBefore)
                .build();

        // Also set individual fields for backward compatibility
        criteria.setFullName(query);
        criteria.setCity(query);
        criteria.setState(query);
        criteria.setCompanyName(query);
        criteria.setJobTitle(query);
        criteria.setInstitution(query);
        criteria.setMajor(query);
        criteria.setDegree(query);

        // Parse skills if provided
        if (query != null && !query.trim().isEmpty()) {
            List<String> skills = List.of(query.split("\\s*,\\s*"));
            criteria.setProgrammingLanguages(skills);
            criteria.setFrameworks(skills);
            criteria.setDatabases(skills);
            criteria.setTools(skills);
            criteria.setCloudTechnologies(skills);
        }

        // Perform search
        List<Resume> searchResults = resumeService.searchResumes(criteria);

        // Format file sizes, prepare display data, and set masked content if needed
        boolean isAuthenticated = isAuthenticated();
        for (Resume resume : searchResults) {
            if (resume.getOriginalFileSize() != null) {
                resume.setFormattedFileSize(formatFileSize(resume.getOriginalFileSize()));
            }

            // Set masked content for unauthorized users
            if (!isAuthenticated) {
                String maskedContent = resumeService.getResumeHtmlContent(resume.getId(), true);
                resume.setHtmlContent(maskedContent);
            }
        }

        model.addAttribute("resumes", searchResults);
        model.addAttribute("searchCriteria", criteria);
        model.addAttribute("resultCount", searchResults.size());
        model.addAttribute("searchQuery", query);
        model.addAttribute("isAuthenticated", isAuthenticated);

        return "search-results";
    }

    /**
     * Search resumes (API endpoint)
     */
    @PostMapping("/api/search")
    @ResponseBody
    public ResponseEntity<?> searchResumesApi(@RequestBody ResumeSearchCriteria criteria) {
        try {
            if (criteria == null) {
                return ResponseEntity.badRequest().body("Search criteria cannot be null");
            }

            List<Resume> results = resumeService.searchResumes(criteria);
            boolean isAuthenticated = isAuthenticated();

            // Process each result
            results.forEach(resume -> {
                // Format file size
                if (resume.getOriginalFileSize() != null) {
                    resume.setFormattedFileSize(formatFileSize(resume.getOriginalFileSize()));
                }

                // Mask sensitive data for unauthenticated users
                if (!isAuthenticated) {
                    resume.setEmail(resume.getMaskedEmail());
                    resume.setPhone(resume.getMaskedPhone());
                }

                // Truncate summary for search results
                if (resume.getProfessionalSummary() != null && resume.getProfessionalSummary().length() > 150) {
                    resume.setProfessionalSummary(resume.getProfessionalSummary().substring(0, 150) + "...");
                }
            });

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("Error during resume search", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error performing search");
        }
    }

    /**
     * Parse comma-separated skills string into list
     */
    private List<String> parseSkills(String skillsString) {
        if (skillsString == null || skillsString.trim().isEmpty()) {
            return Arrays.asList();
        }

        return Arrays.asList(skillsString.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Format file size safely to prevent arithmetic overflow
     */
    private String formatFileSize(Long fileSizeInBytes) {
        if (fileSizeInBytes == null || fileSizeInBytes <= 0) {
            return "0 B";
        }

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = fileSizeInBytes.doubleValue();

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024.0;
            unitIndex++;
        }

        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(size) + " " + units[unitIndex];
    }

}
