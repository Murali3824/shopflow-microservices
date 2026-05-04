package com.shopflow.admin.service;

import com.shopflow.admin.dto.AuditLogResponse;
import com.shopflow.admin.entity.AuditLog;
import com.shopflow.admin.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void logAction(UUID adminId, String action, String targetType,
                          UUID targetId, String details) {

        AuditLog auditLog = AuditLog.builder()
                .adminId(adminId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit log saved — admin: {}, action: {}, target: {}/{}",
                adminId, action, targetType, targetId);
    }

    public Page<AuditLogResponse> getAuditLogs(UUID adminId, Pageable pageable) {
        return auditLogRepository.findByAdminId(adminId, pageable)
                .map(this::toResponse);
    }

    public List<AuditLogResponse> getAuditLogsByTarget(String targetType, UUID targetId) {
        return auditLogRepository.findByTargetTypeAndTargetId(targetType, targetId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .adminId(auditLog.getAdminId())
                .action(auditLog.getAction())
                .targetType(auditLog.getTargetType())
                .targetId(auditLog.getTargetId())
                .details(auditLog.getDetails())
                .performedAt(auditLog.getPerformedAt())
                .build();
    }
}