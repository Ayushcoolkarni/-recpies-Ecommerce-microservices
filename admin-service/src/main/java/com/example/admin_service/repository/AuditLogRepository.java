package com.example.admin_service.repository;

import com.example.admin_service.entity.AuditLog;
import com.example.admin_service.enums.AdminAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByAdminId(Long adminId);
    List<AuditLog> findByAction(AdminAction action);
    List<AuditLog> findByTargetTypeAndTargetId(String targetType, Long targetId);
}