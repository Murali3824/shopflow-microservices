package com.shopflow.auth.event;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisteredEvent {

    private UUID userId;
    private String fullName;
    private String email;
}