package com.kjr.rpf.repository;

import com.kjr.rpf.model.Resume;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ResumeRepository extends MongoRepository<Resume, String> {
    
    List<Resume> findByOriginalFileName(String fileName);
    
    List<Resume> findByUploadedAtBefore(LocalDateTime date);
    
    List<Resume> findByEmail(String email);
    
    List<Resume> findByFirstNameAndLastName(String firstName, String lastName);
    
    // Basic field searches
    List<Resume> findByFirstNameIgnoreCase(String firstName);
    
    List<Resume> findByLastNameIgnoreCase(String lastName);
    
    List<Resume> findByFirstNameIgnoreCaseOrLastNameIgnoreCase(String firstName, String lastName);
    
    List<Resume> findByPhone(String phone);
    
    List<Resume> findByCityIgnoreCase(String city);
    
    List<Resume> findByStateIgnoreCase(String state);
    
    // Date range search
    List<Resume> findByUploadedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Skills searches - using @Query for nested object searches
    @Query("{'skills.programmingLanguages': {$in: ?0}}")
    List<Resume> findByProgrammingLanguagesIn(List<String> programmingLanguages);
    
    @Query("{'skills.frameworks': {$in: ?0}}")
    List<Resume> findByFrameworksIn(List<String> frameworks);
    
    @Query("{'skills.databases': {$in: ?0}}")
    List<Resume> findByDatabasesIn(List<String> databases);
    
    @Query("{'skills.tools': {$in: ?0}}")
    List<Resume> findByToolsIn(List<String> tools);
    
    @Query("{'skills.cloudTechnologies': {$in: ?0}}")
    List<Resume> findByCloudTechnologiesIn(List<String> cloudTechnologies);
    
    // Regex searches for skills (for anySkill search)
    @Query("{'skills.programmingLanguages': {$regex: ?0, $options: 'i'}}")
    List<Resume> findByProgrammingLanguagesRegex(String skillRegex);
    
    @Query("{'skills.frameworks': {$regex: ?0, $options: 'i'}}")
    List<Resume> findByFrameworksRegex(String skillRegex);
    
    @Query("{'skills.databases': {$regex: ?0, $options: 'i'}}")
    List<Resume> findByDatabasesRegex(String skillRegex);
    
    @Query("{'skills.tools': {$regex: ?0, $options: 'i'}}")
    List<Resume> findByToolsRegex(String skillRegex);
    
    @Query("{'skills.cloudTechnologies': {$regex: ?0, $options: 'i'}}")
    List<Resume> findByCloudTechnologiesRegex(String skillRegex);
    
    // Experience searches - using regex for partial matching
    @Query("{'experience.companyName': {$regex: ?0, $options: 'i'}}")
    List<Resume> findByCompanyNameRegex(String companyName);
    
    @Query("{'experience.jobTitle': {$regex: ?0, $options: 'i'}}")
    List<Resume> findByJobTitleRegex(String jobTitle);
    
    // Education searches - using regex for partial matching
    @Query("{'education.degree': {$regex: ?0, $options: 'i'}}")
    List<Resume> findByDegreeRegex(String degree);
    
    @Query("{'education.institution': {$regex: ?0, $options: 'i'}}")
    List<Resume> findByInstitutionRegex(String institution);
    
    @Query("{'education.major': {$regex: ?0, $options: 'i'}}")
    List<Resume> findByMajorRegex(String major);
    
    // General text search across multiple fields
    @Query("{$or: [\n" +
           "    {'firstName': {$regex: ?0, $options: 'i'}},\n" +
           "    {'lastName': {$regex: ?0, $options: 'i'}},\n" +
           "    {'email': {$regex: ?0, $options: 'i'}},\n" +
           "    {'professionalSummary': {$regex: ?0, $options: 'i'}},\n" +
           "    {'experience.jobTitle': {$regex: ?0, $options: 'i'}},\n" +
           "    {'experience.companyName': {$regex: ?0, $options: 'i'}},\n" +
           "    {'experience.responsibilitiesAndAchievements': {$regex: ?0, $options: 'i'}},\n" +
           "    {'education.degree': {$regex: ?0, $options: 'i'}},\n" +
           "    {'education.institution': {$regex: ?0, $options: 'i'}},\n" +
           "    {'education.major': {$regex: ?0, $options: 'i'}},\n" +
           "    {'skills.programmingLanguages': {$regex: ?0, $options: 'i'}},\n" +
           "    {'skills.frameworks': {$regex: ?0, $options: 'i'}},\n" +
           "    {'skills.databases': {$regex: ?0, $options: 'i'}},\n" +
           "    {'skills.tools': {$regex: ?0, $options: 'i'}},\n" +
           "    {'skills.cloudTechnologies': {$regex: ?0, $options: 'i'}}\n" +
           "]}")
    List<Resume> findByGeneralSearch(String keyword);
    
    // Search HTML content directly (since structured fields may not be populated)
    @Query("{'htmlContent': {$regex: ?0, $options: 'i'}}")
    List<Resume> findByHtmlContent(String keyword);
}
