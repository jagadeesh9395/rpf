package com.kjr.rpf.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Education {

        private String degree;
        private String major;
        private String institution;
        private String city;
        private String state;
        private LocalDate startDate;
        private LocalDate endDate;
        private Double gpa; // Optional
        private String honors; // Optional

}
