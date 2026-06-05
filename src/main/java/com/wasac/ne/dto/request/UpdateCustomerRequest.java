package com.wasac.ne.dto.request;

import com.wasac.ne.enums.Status;
import com.wasac.ne.validation.ValidEmail;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateCustomerRequest {

    @Size(min = 2, max = 100)
    private String fullNames;

    @ValidEmail
    private String email;

    @Pattern(regexp = "^(\\+?2507[2389]|07[2389])[0-9]{7}$", message = "Phone must be a valid Rwanda number (e.g. +250788123456 or 0788123456)")
    private String phoneNumber;

    private LocalDate dateOfBirth;

    @Size(min = 5, max = 255)
    private String address;

    private Status status;
}
