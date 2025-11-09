package com.kjr.rpf.service;

import com.kjr.rpf.dto.ResumeSearchCriteria;
import com.kjr.rpf.model.Resume;
import com.kjr.rpf.repository.ResumeRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final Tika tika = new Tika();
    
    /**
     * Deletes resumes older than the specified number of days
     * @param days Number of days to keep resumes
     * @return Number of resumes deleted
     */
    @Transactional
    public long deleteResumesOlderThan(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        log.info("Deleting resumes older than {} days (before {})", days, cutoffDate);
        List<Resume> oldResumes = resumeRepository.findByUploadedAtBefore(cutoffDate);
        
        if (!oldResumes.isEmpty()) {
            log.info("Found {} resumes to delete", oldResumes.size());
            resumeRepository.deleteAll(oldResumes);
            log.info("Successfully deleted {} old resumes", oldResumes.size());
        } else {
            log.info("No old resumes found to delete");
        }
        
        return oldResumes.size();
    }
    
    /**
     * Delete a single resume by ID
     * @param id The ID of the resume to delete
     * @throws IllegalArgumentException if resume with given ID is not found
     */
    @Transactional
    public void deleteResume(String id) {
        if (!resumeRepository.existsById(id)) {
            throw new IllegalArgumentException("Resume not found with id: " + id);
        }
        resumeRepository.deleteById(id);
        log.info("Deleted resume with id: {}", id);
    }


    @Autowired
    public ResumeService(ResumeRepository resumeRepository) {
        this.resumeRepository = resumeRepository;
    }

    /**
     * Upload and convert document to HTML using Apache Tika
     */
    public Resume uploadAndConvertResume(MultipartFile file, String firstName, String lastName) throws IOException {
        log.info("Processing file: {}", file.getOriginalFilename());

        // Check file size before processing to prevent overflow
        long maxSize = 100 * 1024 * 1024; // 100MB limit
        if (file.getSize() > maxSize) {
            throw new IOException("File size exceeds maximum limit of 100MB: " + file.getSize() + " bytes");
        }

        // Create Resume document
        Resume resume = new Resume();
        resume.setOriginalFileName(file.getOriginalFilename());
        resume.setOriginalFileType(file.getContentType());
        resume.setOriginalFileSize(file.getSize());
        resume.setUploadedAt(LocalDateTime.now());
        resume.setOriginalFileData(file.getBytes());
        
        // Set the candidate's name from the form - save these values before any processing
        String savedFirstName = firstName != null ? firstName.trim() : "";
        String savedLastName = lastName != null ? lastName.trim() : "";
        
        // Convert to HTML using Tika
        String htmlContent = convertToHtml(file.getBytes());
        resume.setHtmlContent(htmlContent);
        
        // Extract and set contact information (email and phone) from the parsed text
        // Skip name extraction since we're getting it from the form
        extractContactInfo(resume, htmlContent);
        
        // Ensure the names from the form are preserved and not overridden
        resume.setFirstName(savedFirstName);
        resume.setLastName(savedLastName);

        // Save to MongoDB
        Resume savedResume = resumeRepository.save(resume);
        log.info("Resume saved with ID: {}", savedResume.getId());

        return savedResume;
    }

    /**
     * Convert document to HTML format using Apache Tika
     */
    private String convertToHtml(byte[] fileData) throws IOException {
        try (InputStream inputStream = new ByteArrayInputStream(fileData)) {
            // Create parser and metadata
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(100 * 1024 * 1024); // 100MB limit to prevent overflow
            Metadata metadata = new Metadata();
            ParseContext parseContext = new ParseContext();

            // Parse document
            parser.parse(inputStream, handler, metadata, parseContext);

            // Get text content and wrap in HTML
            String textContent = handler.toString();

            // Create formatted HTML
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<!DOCTYPE html>\n");
            htmlBuilder.append("<html>\n<head>\n");
            htmlBuilder.append("<meta charset=\"UTF-8\">\n");
            htmlBuilder.append("<title>Resume</title>\n");
            htmlBuilder.append("<style>\n");
            htmlBuilder.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.8; padding: 40px; max-width: 1000px; margin: 0 auto; background: #f5f5f5; color: #333; }\n");
            htmlBuilder.append("pre { white-space: pre-wrap; word-wrap: break-word; font-family: 'Segoe UI', Arial, sans-serif; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); font-size: 14px; line-height: 1.8; }\n");
            htmlBuilder.append("h1 { font-size: 28px; font-weight: 700; color: #2c3e50; margin: 20px 0 10px 0; border-bottom: 3px solid #667eea; padding-bottom: 10px; }\n");
            htmlBuilder.append("h2 { font-size: 24px; font-weight: 600; color: #34495e; margin: 18px 0 8px 0; border-bottom: 2px solid #95a5a6; padding-bottom: 8px; }\n");
            htmlBuilder.append("h3 { font-size: 20px; font-weight: 600; color: #4a5568; margin: 15px 0 8px 0; }\n");
            htmlBuilder.append("h4 { font-size: 18px; font-weight: 600; color: #5a6c7d; margin: 12px 0 6px 0; }\n");
            htmlBuilder.append("p { margin: 10px 0; line-height: 1.8; }\n");
            htmlBuilder.append("table { border-collapse: collapse; width: 100%; margin: 15px 0; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n");
            htmlBuilder.append("th { background: #667eea; color: white; padding: 12px; text-align: left; font-weight: 600; border: 1px solid #5568d3; }\n");
            htmlBuilder.append("td { padding: 10px 12px; border: 1px solid #e1e8ed; }\n");
            htmlBuilder.append("tr:nth-child(even) { background: #f8f9fa; }\n");
            htmlBuilder.append("tr:hover { background: #e9ecef; }\n");
            htmlBuilder.append("ul, ol { margin: 10px 0; padding-left: 30px; }\n");
            htmlBuilder.append("li { margin: 5px 0; line-height: 1.6; }\n");
            htmlBuilder.append("strong, b { font-weight: 600; color: #2c3e50; }\n");
            htmlBuilder.append("em, i { font-style: italic; color: #5a6c7d; }\n");
            htmlBuilder.append("a { color: #667eea; text-decoration: none; }\n");
            htmlBuilder.append("a:hover { text-decoration: underline; }\n");
            htmlBuilder.append("</style>\n");
            htmlBuilder.append("</head>\n<body>\n");
            htmlBuilder.append("<pre>").append(escapeHtml(textContent)).append("</pre>\n");
            htmlBuilder.append("</body>\n</html>");

            return htmlBuilder.toString();

        } catch (SAXException | TikaException e) {
            log.error("Error converting document to HTML", e);
            if (e.getMessage() != null && e.getMessage().contains("limit")) {
                throw new IOException("Document is too large to process (exceeds 100MB limit)", e);
            }
            throw new IOException("Failed to convert document: " + e.getMessage(), e);
        }
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Get resume by ID
     */
    public Optional<Resume> getResumeById(String id) {
        return resumeRepository.findById(id);
    }

    /**
     * Get all resumes
     */
    public List<Resume> getAllResumes() {
        return resumeRepository.findAll();
    }


    /**
     * Mask personal information in text
     * Only masks email addresses and phone numbers, leaves all other content as is
     */
    private String maskPersonalInfo(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // Mask email addresses - keep first 3 chars, then ***@mail
        text = text.replaceAll("(?i)([a-zA-Z0-9._%+-]{3})[a-zA-Z0-9._%+-]*@[a-z0-9.-]+\\.[a-z]{2,}", "$1***@mail");
        
        // Mask phone numbers - keep last 4 digits, mask the rest with *
        text = text.replaceAll("(\\+?\\(?\\d{1,3}\\)?[-.\s]?)?\\d{2,3}[-.\s]?\\d{2,3}[-.\s]?(\\d{4})", "******$2");
        
        return text;
    }
    
    /**
     * Get resume HTML content for viewing with optional masking of personal information
     * @param id The resume ID
     * @param maskPersonalInfo Whether to mask personal information
     * @return The HTML content with optional masking
     */
    public String getResumeHtmlContent(String id, boolean maskPersonalInfo) {
        Optional<Resume> resumeOpt = resumeRepository.findById(id);
        if (resumeOpt.isEmpty()) {
            return null;
        }
        
        String htmlContent = resumeOpt.get().getHtmlContent();
        
        if (maskPersonalInfo) {
            // Extract the content between <pre> tags to avoid breaking HTML
            Pattern pattern = Pattern.compile("(?s)(?<=<pre>)(.*?)(?=</pre>)");
            Matcher matcher = pattern.matcher(htmlContent);
            StringBuffer sb = new StringBuffer();
            
            while (matcher.find()) {
                String matchedText = matcher.group(1);
                String processedText = escapeHtml(maskPersonalInfo(matchedText));
                matcher.appendReplacement(sb, Matcher.quoteReplacement(processedText));
            }
            matcher.appendTail(sb);
            htmlContent = sb.toString();
        }
        
        return htmlContent;
    }
    
    /**
     * Get resume HTML content for viewing without masking
     */
    public String getResumeHtmlContent(String id) {
        return getResumeHtmlContent(id, false);
    }

    /**
     * Search resumes by various criteria
     */
    public List<Resume> searchResumes(ResumeSearchCriteria criteria) {
        log.info("Searching resumes with criteria: {}", criteria);

        List<Resume> results = new ArrayList<>();

        // If no criteria specified, return all resumes
        if (!hasAnyCriteria(criteria)) {
            return resumeRepository.findAll();
        }

        // Try specific searches first
        boolean hasResults = false;

        // Basic text search across multiple fields - search HTML content since structured fields may not be populated
        if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
            List<Resume> keywordResults = resumeRepository.findByHtmlContent(criteria.getKeyword());
            log.debug("Keyword search for '{}' returned {} results", criteria.getKeyword(), keywordResults.size());
            if (!keywordResults.isEmpty()) {
                results.addAll(keywordResults);
                hasResults = true;
            }
        }

        // Name search (OR logic within name fields)
        if (criteria.getFirstName() != null && !criteria.getFirstName().trim().isEmpty()) {
            List<Resume> nameResults = resumeRepository.findByFirstNameIgnoreCase(criteria.getFirstName());
            log.debug("First name search for '{}' returned {} results", criteria.getFirstName(), nameResults.size());
            if (!nameResults.isEmpty()) {
                results.addAll(nameResults);
                hasResults = true;
            }
        }

        if (criteria.getLastName() != null && !criteria.getLastName().trim().isEmpty()) {
            List<Resume> nameResults = resumeRepository.findByLastNameIgnoreCase(criteria.getLastName());
            log.debug("Last name search for '{}' returned {} results", criteria.getLastName(), nameResults.size());
            if (!nameResults.isEmpty()) {
                results.addAll(nameResults);
                hasResults = true;
            }
        }

        if (criteria.getFullName() != null && !criteria.getFullName().trim().isEmpty()) {
            List<Resume> nameResults = resumeRepository.findByFirstNameIgnoreCaseOrLastNameIgnoreCase(
                    criteria.getFullName(), criteria.getFullName());
            log.debug("Full name search for '{}' returned {} results", criteria.getFullName(), nameResults.size());
            if (!nameResults.isEmpty()) {
                results.addAll(nameResults);
                hasResults = true;
            }
        }

        // Contact search
        if (criteria.getEmail() != null && !criteria.getEmail().trim().isEmpty()) {
            List<Resume> emailResults = resumeRepository.findByEmail(criteria.getEmail());
            if (!emailResults.isEmpty()) {
                results.addAll(emailResults);
                hasResults = true;
            }
        }

        if (criteria.getPhone() != null && !criteria.getPhone().trim().isEmpty()) {
            // Search phone in HTML content since structured phone field may not be populated
            List<Resume> phoneResults = resumeRepository.findByHtmlContent(criteria.getPhone());
            if (!phoneResults.isEmpty()) {
                results.addAll(phoneResults);
                hasResults = true;
            }
        }

        // Location search
        if (criteria.getCity() != null && !criteria.getCity().trim().isEmpty()) {
            List<Resume> cityResults = resumeRepository.findByCityIgnoreCase(criteria.getCity());
            if (!cityResults.isEmpty()) {
                results.addAll(cityResults);
                hasResults = true;
            }
        }

        if (criteria.getState() != null && !criteria.getState().trim().isEmpty()) {
            List<Resume> stateResults = resumeRepository.findByStateIgnoreCase(criteria.getState());
            if (!stateResults.isEmpty()) {
                results.addAll(stateResults);
                hasResults = true;
            }
        }

        // Skills search
        if (criteria.getProgrammingLanguages() != null && !criteria.getProgrammingLanguages().isEmpty()) {
            List<Resume> skillResults = resumeRepository.findByProgrammingLanguagesIn(criteria.getProgrammingLanguages());
            if (!skillResults.isEmpty()) {
                results.addAll(skillResults);
                hasResults = true;
            }
        }

        if (criteria.getFrameworks() != null && !criteria.getFrameworks().isEmpty()) {
            List<Resume> skillResults = resumeRepository.findByFrameworksIn(criteria.getFrameworks());
            if (!skillResults.isEmpty()) {
                results.addAll(skillResults);
                hasResults = true;
            }
        }

        // Experience search
        if (criteria.getCompanyName() != null && !criteria.getCompanyName().trim().isEmpty()) {
            List<Resume> expResults = resumeRepository.findByCompanyNameRegex(criteria.getCompanyName());
            if (!expResults.isEmpty()) {
                results.addAll(expResults);
                hasResults = true;
            }
        }

        if (criteria.getJobTitle() != null && !criteria.getJobTitle().trim().isEmpty()) {
            List<Resume> expResults = resumeRepository.findByJobTitleRegex(criteria.getJobTitle());
            if (!expResults.isEmpty()) {
                results.addAll(expResults);
                hasResults = true;
            }
        }

        // Education search
        if (criteria.getDegree() != null && !criteria.getDegree().trim().isEmpty()) {
            List<Resume> eduResults = resumeRepository.findByDegreeRegex(criteria.getDegree());
            if (!eduResults.isEmpty()) {
                results.addAll(eduResults);
                hasResults = true;
            }
        }

        if (criteria.getInstitution() != null && !criteria.getInstitution().trim().isEmpty()) {
            List<Resume> eduResults = resumeRepository.findByInstitutionRegex(criteria.getInstitution());
            if (!eduResults.isEmpty()) {
                results.addAll(eduResults);
                hasResults = true;
            }
        }

        // Date range search
        if ((criteria.getUploadedAfter() != null && !criteria.getUploadedAfter().trim().isEmpty()) ||
            (criteria.getUploadedBefore() != null && !criteria.getUploadedBefore().trim().isEmpty())) {
            LocalDateTime startDate = criteria.getUploadedAfter() != null && !criteria.getUploadedAfter().trim().isEmpty() ?
                    LocalDateTime.parse(criteria.getUploadedAfter()) : LocalDateTime.MIN;
            LocalDateTime endDate = criteria.getUploadedBefore() != null && !criteria.getUploadedBefore().trim().isEmpty() ?
                    LocalDateTime.parse(criteria.getUploadedBefore()) : LocalDateTime.now();

            List<Resume> dateResults = resumeRepository.findByUploadedAtBetween(startDate, endDate);
            if (!dateResults.isEmpty()) {
                results.addAll(dateResults);
                hasResults = true;
            }
        }

        // If no specific searches returned results, return empty list instead of all resumes
        if (!hasResults) {
            log.info("No search results found for criteria: {}, returning empty results", criteria);
            return new ArrayList<>();
        }

        // Remove duplicates and return
        List<Resume> finalResults = results.stream()
                .distinct()
                .collect(Collectors.toList());

        log.info("Search completed. Total results: {} (from {} initial matches)", finalResults.size(), results.size());
        return finalResults;
    }

    /**
     * Extract and set contact information (email and phone) from HTML content
     * @param resume The resume to update with contact information
     * @param htmlContent The HTML content to extract information from
     */
    private void extractContactInfo(Resume resume, String htmlContent) {
        // First, get plain text content without HTML tags
        String textContent = htmlContent.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        
        // Only extract email if not already set
        if (resume.getEmail() == null || resume.getEmail().isEmpty()) {
            // Extract email with more precise pattern
            Pattern emailPattern = Pattern.compile("\\bE[-]?mail\\s*[:]?\\s*([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b");
            Matcher emailMatcher = emailPattern.matcher(textContent);
            if (emailMatcher.find()) {
                String email = emailMatcher.group(1).trim();
                resume.setEmail(email);
            } else {
                // Fallback to simple email pattern if the above doesn't match
                emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
                emailMatcher = emailPattern.matcher(textContent);
                if (emailMatcher.find()) {
                    String email = emailMatcher.group().trim();
                    resume.setEmail(email);
                }
            }
        }
        
        // Only extract phone if not already set
        if (resume.getPhone() == null || resume.getPhone().isEmpty()) {
            // Try different phone number patterns
            String[] phonePatterns = {
                // Pattern for phone numbers with labels (e.g., "Phone: +91-7338993710")
                "(?i)(?:Mobile|Phone|Tel|Mob)[:\\s]+([+\\d\\s\\(\\)\\-]{10,20})",
                // Pattern for standalone phone numbers with country code (e.g., "+91-7338993710" or "+917338993710")
                "\\+?[0-9]{1,3}[-.\\s]?[0-9]{5}[-.\\s]?[0-9]{5}",
                // Pattern for standard 10-digit Indian mobile numbers
                "(?:\\+?91[\\s-]?)?[6-9]\\d{9}",
                // Pattern for numbers in parentheses or with spaces/dashes
                "\\(?([0-9]{3})\\)?[-.\\s]?([0-9]{3})[-.\\s]?([0-9]{4})"
            };

            String phone = null;
            
            // Try each pattern until we find a match
            for (String patternStr : phonePatterns) {
                Pattern phonePattern = Pattern.compile(patternStr);
                Matcher phoneMatcher = phonePattern.matcher(textContent);
                
                if (phoneMatcher.find()) {
                    // Clean up the phone number - remove all non-digit characters
                    phone = phoneMatcher.group().replaceAll("[^0-9]", "");
                    
                    // If we have a match, process it and break the loop
                    if (phone != null && phone.length() >= 10) {
                        break;
                    }
                }
            }
            
            // If we found a phone number, clean it up
            if (phone != null && !phone.isEmpty()) {
                // Remove country codes (like +91, 91, 0091 for India)
                if (phone.startsWith("91") && phone.length() > 10) {
                    phone = phone.substring(2);
                } else if (phone.startsWith("0") && phone.length() > 10) {
                    phone = phone.substring(1);
                }
                
                // Ensure we have exactly 10 digits (for Indian numbers)
                if (phone.length() >= 10) {
                    // Take only the last 10 digits if it's longer
                    if (phone.length() > 10) {
                        phone = phone.substring(phone.length() - 10);
                    }
                    
                    // Basic validation for Indian mobile numbers
                    if (phone.matches("[6-9]\\d{9}")) {
                        resume.setPhone(phone);
                    }
                }
            }
        }
    }
    
    /**
     * Extract and set personal information from HTML content
     * @deprecated Use extractContactInfo instead, as we now get name from the form
     */
//    @Deprecated
//    private void extractAndSetPersonalInfo(Resume resume, String htmlContent) {
//        // Just call extractContactInfo for backward compatibility
//        extractContactInfo(resume, htmlContent);
//    } else {
//            // Fallback to simple phone pattern
//            phonePattern = Pattern.compile("(\\+\\d{1,3}[-.\\(\\s]?)?\\(?\\d{3}\\)?[-.\\(\\s]?\\d{3}[-.\\(\\s]?\\d{4}");
//            phoneMatcher = phonePattern.matcher(textContent);
//            if (phoneMatcher.find()) {
//                String phone = phoneMatcher.group().trim();
//                // Clean the phone number and remove country code
//                phone = phone.replaceAll("[^0-9+]", "");
//
//                // Remove country codes (like +91, 91, 0091 for India)
//                if (phone.startsWith("+")) {
//                    if (phone.startsWith("+91") && phone.length() > 12) {
//                        phone = phone.substring(3);
//                    }
//                } else if (phone.startsWith("91") && phone.length() > 10) {
//                    phone = phone.substring(2);
//                } else if (phone.startsWith("0091") && phone.length() > 11) {
//                    phone = phone.substring(4);
//                }
//
//                if (phone.length() >= 10) {
//                    if (phone.length() > 10) {
//                        phone = phone.substring(phone.length() - 10);
//                    }
//                    resume.setPhone(phone);
//                }
//            }
//        }
//
//        // First, try to extract name from the document content
//        boolean nameFound = false;
//
//        // Pattern 1: Look for a line that looks like a name at the beginning of the document
//        String[] lines = textContent.split("\\r?\\n");
//        for (String line : lines) {
//            line = line.trim();
//            // Check if line looks like a name (starts with capital, has at least 2 words, no numbers, not too long)
//            if (line.matches("^[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+$") && line.length() <= 50) {
//                String[] nameParts = line.split("\\s+");
//                if (nameParts.length >= 2) {
//                    resume.setFirstName(nameParts[0]);
//                    resume.setLastName(nameParts[nameParts.length - 1]);
//                    nameFound = true;
//                    log.debug("Found name in document: {} {}", resume.getFirstName(), resume.getLastName());
//                    break;
//                } else if (nameParts.length == 1) {
//                    resume.setFirstName(nameParts[0]);
//                    nameFound = true;
//                    log.debug("Found first name in document: {}", resume.getFirstName());
//                    break;
//                }
//            }
//        }
//
//        // If name not found in document, try to extract from email (only if email exists)
//        if (!nameFound && email != null) {
//            String emailPrefix = email.split("@")[0];
//            // Try to extract name from email if it follows common patterns
//            if (emailPrefix.matches(".*\\.(com|org|net|io|co|in)$")) {
//                // Skip if it's a domain-like prefix
//                log.debug("Skipping email prefix as it looks like a domain: {}", emailPrefix);
//            } else {
//                // Try to split by common separators
//                String[] nameParts = emailPrefix.split("[._-]");
//                if (nameParts.length >= 2) {
//                    // If we have at least two parts, assume first and last name
//                    String firstName = nameParts[0];
//                    String lastName = nameParts[nameParts.length - 1];
//
//                    // Basic validation - names should be at least 2 characters and not too long
//                    if (firstName.length() >= 2 && firstName.length() <= 20 &&
//                        lastName.length() >= 2 && lastName.length() <= 20) {
//
//                        // Capitalize first letter of each name part
//                        firstName = firstName.substring(0, 1).toUpperCase() +
//                                  (firstName.length() > 1 ? firstName.substring(1) : "");
//                        lastName = lastName.substring(0, 1).toUpperCase() +
//                                 (lastName.length() > 1 ? lastName.substring(1) : "");
//
//                        resume.setFirstName(firstName);
//                        resume.setLastName(lastName);
//                        nameFound = true;
//                        log.debug("Extracted name from email: {} {}", firstName, lastName);
//                    }
//                } else if (nameParts.length == 1 && nameParts[0].length() >= 2 && nameParts[0].length() <= 20) {
//                    // If only one part, use it as first name
//                    String firstName = nameParts[0].substring(0, 1).toUpperCase() +
//                                     (nameParts[0].length() > 1 ? nameParts[0].substring(1) : "");
//                    resume.setFirstName(firstName);
//                    nameFound = true;
//                    log.debug("Extracted first name from email: {}", firstName);
//                }
//            }
//        }
//
//        // If still no name found, log a warning
//        if (!nameFound) {
//            log.warn("Could not extract name from resume. Email: {}", email);
//        }
//
//        // If we still don't have a name but have an email, use the part before @ as first name
//        if ((resume.getFirstName() == null || resume.getFirstName().isEmpty()) && resume.getEmail() != null) {
//            String emailName = resume.getEmail().split("@")[0];
//            if (emailName.contains(".")) {
//                String[] parts = emailName.split("\\.");
//                resume.setFirstName(parts[0]);
//                if (parts.length > 1) {
//                    resume.setLastName(parts[parts.length - 1]);
//                }
//            } else {
//                resume.setFirstName(emailName);
//            }
//        }
//    }
    
    /**
     * Check if search criteria has any non-null values
     */
    private boolean hasAnyCriteria(ResumeSearchCriteria criteria) {
        return criteria.getFirstName() != null || criteria.getLastName() != null ||
                criteria.getFullName() != null || criteria.getEmail() != null ||
                criteria.getPhone() != null || criteria.getCity() != null ||
                criteria.getState() != null || criteria.getProgrammingLanguages() != null ||
                criteria.getFrameworks() != null || criteria.getCompanyName() != null ||
                criteria.getJobTitle() != null || criteria.getDegree() != null ||
                criteria.getInstitution() != null || criteria.getKeyword() != null ||
                (criteria.getUploadedAfter() != null && !criteria.getUploadedAfter().trim().isEmpty()) ||
                (criteria.getUploadedBefore() != null && !criteria.getUploadedBefore().trim().isEmpty());
    }
    
    /**
     * Save or update a resume in the database
     * @param resume The resume to save or update
     * @return The saved resume
     */
    public Resume saveResume(Resume resume) {
        if (resume == null) {
            throw new IllegalArgumentException("Resume cannot be null");
        }
        return resumeRepository.save(resume);
    }
}
