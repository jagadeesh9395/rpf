package com.kjr.rpf.controller;

import com.kjr.rpf.dto.ResumeSearchCriteria;
import com.kjr.rpf.model.Resume;
import com.kjr.rpf.service.ResumeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

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
    @GetMapping("/list")
    public String showAllResumes(Model model) {
        List<Resume> resumes = resumeService.getAllResumes();

        // Format file sizes to prevent arithmetic overflow in template
        for (Resume resume : resumes) {
            if (resume.getOriginalFileSize() != null) {
                resume.setFormattedFileSize(formatFileSize(resume.getOriginalFileSize()));
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
    public String uploadResume(@RequestParam("file") MultipartFile file, Model model) {
        try {
            if (file.isEmpty()) {
                model.addAttribute("error", "Please select a file to upload");
                return "upload";
            }

            Resume resume = resumeService.uploadAndConvertResume(file);
            model.addAttribute("success", "File uploaded successfully!");
            model.addAttribute("resumeId", resume.getId());

            return "redirect:/resumes/view/" + resume.getId();

        } catch (IOException e) {
            log.error("Error uploading file", e);
            model.addAttribute("error", "Failed to upload file: " + e.getMessage());
            return "upload";
        }
    }

    /**
     * View resume in HTML format
     */
    @GetMapping("/view/{id}")
    public String viewResume(@PathVariable String id, Model model) {
        String htmlContent = resumeService.getResumeHtmlContent(id);

        if (htmlContent == null) {
            model.addAttribute("error", "Resume not found");
            model.addAttribute("htmlContent", null);
        } else {
            model.addAttribute("error", null);
            model.addAttribute("htmlContent", htmlContent);
        }

        model.addAttribute("resumeId", id);
        return "viewer";
    }

    /**
     * Download original resume file
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadResume(@PathVariable String id) {
        try {
            Resume resume = resumeService.getResumeById(id)
                    .orElseThrow(() -> new RuntimeException("Resume not found"));

            byte[] fileData = resume.getOriginalFileData();
            ByteArrayResource resource = new ByteArrayResource(fileData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resume.getOriginalFileName() + "\"")
                    .contentType(MediaType.parseMediaType(resume.getOriginalFileType()))
                    .contentLength(fileData.length)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading resume", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get all resumes (API endpoint)
     */
    @GetMapping("/api/all")
    @ResponseBody
    public ResponseEntity<List<Resume>> getAllResumes() {
        List<Resume> resumes = resumeService.getAllResumes();

        // Format file sizes to prevent arithmetic overflow
        for (Resume resume : resumes) {
            if (resume.getOriginalFileSize() != null) {
                resume.setFormattedFileSize(formatFileSize(resume.getOriginalFileSize()));
            }
        }

        return ResponseEntity.ok(resumes);
    }

    /**
     * Get resume by ID (API endpoint)
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Resume> getResumeById(@PathVariable String id) {
        return resumeService.getResumeById(id)
                .map(resume -> {
                    // Format file size to prevent arithmetic overflow
                    if (resume.getOriginalFileSize() != null) {
                        resume.setFormattedFileSize(formatFileSize(resume.getOriginalFileSize()));
                    }
                    return ResponseEntity.ok(resume);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete resume
     */
    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteResume(@PathVariable String id) {
        resumeService.deleteResume(id);
        return ResponseEntity.noContent().build();
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
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String programmingLanguages,
            @RequestParam(required = false) String frameworks,
            @RequestParam(required = false) String databases,
            @RequestParam(required = false) String tools,
            @RequestParam(required = false) String cloudTechnologies,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String degree,
            @RequestParam(required = false) String institution,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String uploadedAfter,
            @RequestParam(required = false) String uploadedBefore,
            Model model) {

        // Parse comma-separated skills into lists
        List<String> progLangList = parseSkills(programmingLanguages);
        List<String> frameworksList = parseSkills(frameworks);
        List<String> databasesList = parseSkills(databases);
        List<String> toolsList = parseSkills(tools);
        List<String> cloudTechList = parseSkills(cloudTechnologies);

        // Create search criteria
        ResumeSearchCriteria criteria = ResumeSearchCriteria.builder()
                .firstName(firstName)
                .lastName(lastName)
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .city(city)
                .state(state)
                .programmingLanguages(progLangList)
                .frameworks(frameworksList)
                .databases(databasesList)
                .tools(toolsList)
                .cloudTechnologies(cloudTechList)
                .companyName(companyName)
                .jobTitle(jobTitle)
                .degree(degree)
                .institution(institution)
                .major(major)
                .keyword(keyword)
                .uploadedAfter(uploadedAfter)
                .uploadedBefore(uploadedBefore)
                .build();

        // Perform search
        List<Resume> searchResults = resumeService.searchResumes(criteria);

        // Format file sizes to prevent arithmetic overflow in template
        for (Resume resume : searchResults) {
            if (resume.getOriginalFileSize() != null) {
                resume.setFormattedFileSize(formatFileSize(resume.getOriginalFileSize()));
            }
        }

        model.addAttribute("resumes", searchResults);
        model.addAttribute("searchCriteria", criteria);
        model.addAttribute("resultCount", searchResults.size());

        return "search-results";
    }

    /**
     * Search resumes (API endpoint)
     */
    @PostMapping("/api/search")
    @ResponseBody
    public ResponseEntity<List<Resume>> searchResumesApi(@RequestBody ResumeSearchCriteria criteria) {
        List<Resume> results = resumeService.searchResumes(criteria);

        // Format file sizes to prevent arithmetic overflow
        for (Resume resume : results) {
            if (resume.getOriginalFileSize() != null) {
                resume.setFormattedFileSize(formatFileSize(resume.getOriginalFileSize()));
            }
        }

        return ResponseEntity.ok(results);
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
