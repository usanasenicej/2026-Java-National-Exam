package com.wasac.ne.dto.request;

import com.wasac.ne.enums.Status;
import com.wasac.ne.validation.ValidEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Create customer")
public class CreateCustomerRequest {

    @NotBlank @Size(min = 2, max = 100)
    private String fullNames;

    @NotBlank
    @Pattern(regexp = "^[0-9]{16}$", message = "National ID must be exactly 16 digits")
    private String nationalId;

    @NotBlank @ValidEmail
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\+?250[0-9]{9}$|^0[0-9]{9}$")
    private String phoneNumber;

    @NotBlank @Size(min = 5, max = 255)
    private String address;

    @NotBlank
    private Status status;
}
