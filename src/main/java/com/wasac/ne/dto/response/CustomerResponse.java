package com.wasac.ne.dto.response;

import com.wasac.ne.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerResponse {

    private Long id;
    private String fullNames;
    private String nationalId;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String address;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
