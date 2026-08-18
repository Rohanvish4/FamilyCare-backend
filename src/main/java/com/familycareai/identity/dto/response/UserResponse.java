package com.familycareai.identity.dto.response;

import com.familycareai.identity.entity.AccountStatus;
import com.familycareai.identity.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private Gender gender;
    private AccountStatus accountStatus;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Set<String> roles;
    private Instant createdAt;
}
