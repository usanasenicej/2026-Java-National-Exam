package com.wasac.ne.dto.request;

import com.wasac.ne.enums.Status;
import com.wasac.ne.validation.ValidEmail;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCustomerRequest {

    @Size(min = 2, max = 100)
    private String fullNames;

    @ValidEmail
    private String email;

    @Pattern(regexp = "^\\+?250[0-9]{9}$|^0[0-9]{9}$")
    private String phoneNumber;

    @Size(min = 5, max = 255)
    private String address;

    private Status status;
}
