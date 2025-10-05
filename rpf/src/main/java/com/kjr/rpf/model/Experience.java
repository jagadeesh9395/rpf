package com.kjr.rpf.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class Experience {

        private String jobTitle;
        private String companyName;
        private String city;
        private String state;
        private LocalDate startDate;
        private LocalDate endDate;
        private boolean isCurrentPosition;
        private List<String> responsibilitiesAndAchievements;



}
