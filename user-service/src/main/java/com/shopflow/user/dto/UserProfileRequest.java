package com.shopflow.user.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileRequest {

    @Size(max = 100)
    private String fullName;

    @Size(max = 20)
    private String phone;

    private LocalDate dateOfBirth;
}