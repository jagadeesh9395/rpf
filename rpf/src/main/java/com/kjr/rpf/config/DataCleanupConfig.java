package com.kjr.rpf.config;

import com.kjr.rpf.service.ResumeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@Slf4j
public class DataCleanupConfig {

    private final ResumeService resumeService;
    private final int retentionDays;

    public DataCleanupConfig(ResumeService resumeService,
                           @Value("${app.retention.days:180}") int retentionDays) {
        this.resumeService = resumeService;
        this.retentionDays = retentionDays;
    }

    /**
     * Scheduled task to delete old resumes
     * Runs every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2 AM
    public void cleanupOldResumes() {
        try {
            log.info("Starting scheduled cleanup of resumes older than {} days", retentionDays);
            long deletedCount = resumeService.deleteResumesOlderThan(retentionDays);
            log.info("Completed cleanup. Deleted {} old resumes", deletedCount);
        } catch (Exception e) {
            log.error("Error during scheduled cleanup of old resumes", e);
        }
    }
}
