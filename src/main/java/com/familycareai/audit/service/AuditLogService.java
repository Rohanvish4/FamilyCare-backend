package com.familycareai.audit.service;

import com.familycareai.audit.entity.AuditAction;
import com.familycareai.audit.entity.AuditLog;
import com.familycareai.audit.entity.AuditStatus;
import com.familycareai.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(UUID actorUserId,
                          AuditAction action,
                          String ipAddress,
                          String userAgent,
                          AuditStatus status,
                          Map<String, Object> metadataMap) {
        try {
            String metadataJson = null;
            if (metadataMap != null && !metadataMap.isEmpty()) {
                metadataJson = objectMapper.writeValueAsString(metadataMap);
            }

            AuditLog logEntry = AuditLog.builder()
                    .actorUserId(actorUserId)
                    .action(action)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status(status)
                    .metadata(metadataJson)
                    .build();

            auditLogRepository.save(logEntry);
            log.debug("Logged audit action: {} for user: {} with status: {}", action, actorUserId, status);
        } catch (Exception e) {
            log.error("Failed to write audit log entry for action: {}", action, e);
        }
    }
}
