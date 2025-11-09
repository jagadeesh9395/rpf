package com.kjr.rpf.controller;

import com.kjr.rpf.dto.ResumeSearchCriteria;
import com.kjr.rpf.model.Resume;
import com.kjr.rpf.service.ResumeService;
import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("/export")
@Slf4j
public class ExportController {

    private final ResumeService resumeService;

    @Autowired
    public ExportController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @GetMapping("/search-results")
    public void exportSearchResultsToCsv(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String uploadedBefore,
            HttpServletResponse response) throws Exception {

        // Set response headers
        String filename = "resume_search_results_" + System.currentTimeMillis() + ".csv";
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
            "attachment; filename=\"" + filename + "\"");

        // Create search criteria
        ResumeSearchCriteria criteria = new ResumeSearchCriteria();
        if (query != null && !query.trim().isEmpty()) {
            criteria.setKeyword(query);
        }
        if (uploadedBefore != null) {
            criteria.setUploadedBefore(uploadedBefore);
        }

        // Get search results
        List<Resume> searchResults = resumeService.searchResumes(criteria);

        // Write CSV response
        try (CSVWriter writer = new CSVWriter(response.getWriter())) {
            // Write CSV header
            String[] header = {"Name", "Email", "Phone", "Search Term"};
            writer.writeNext(header);

            // Write data rows with each resume's individual name
            for (Resume resume : searchResults) {
                // Build the name from first and last name
                StringBuilder nameBuilder = new StringBuilder();
                if (resume.getFirstName() != null && !resume.getFirstName().trim().isEmpty()) {
                    nameBuilder.append(resume.getFirstName().trim());
                    if (resume.getLastName() != null && !resume.getLastName().trim().isEmpty()) {
                        nameBuilder.append(" ").append(resume.getLastName().trim());
                    }
                } else if (resume.getEmail() != null) {
                    // Fallback to email username if name is not available
                    nameBuilder.append(resume.getEmail().split("@")[0]);
                } else {
                    nameBuilder.append("Resume Candidate");
                }
                
                String[] row = new String[]{
                    nameBuilder.toString(),
                    resume.getEmail() != null ? resume.getEmail() : "",
                    resume.getPhone() != null ? resume.getPhone() : "",
                    query != null ? query : ""
                };
                writer.writeNext(row);
            }
        } catch (Exception e) {
            log.error("Error generating CSV export", e);
            throw new Exception("Error generating CSV export", e);
        }
    }
}
