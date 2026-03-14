package com.example.admin_service.dto.response;

import com.example.admin_service.enums.AdminAction;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder
public class AuditLogResponse {
    private Long id;
    private Long adminId;
    private AdminAction action;
    private String targetType;
    private Long targetId;
    private String details;
    private LocalDateTime timestamp;
}