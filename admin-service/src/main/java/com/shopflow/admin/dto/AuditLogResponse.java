package com.shopflow.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class AuditLogResponse {
    private UUID id;
    private UUID adminId;
    private String action;
    private String targetType;
    private UUID targetId;
    private String details;
    private ZonedDateTime performedAt;
}