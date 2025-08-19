package org.openmrs.module.auditlogweb.api.utils;

import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.dto.RestAuditLogDto;
import org.openmrs.api.context.Context;
import org.openmrs.User;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuditLogMapper {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z")
            .withZone(ZoneId.of("GMT"));

    public RestAuditLogDto toDto(AuditEntity<?> audit) {
        String formattedDate = formatter.format(audit.getRevisionEntity().getRevisionDate().toInstant());
        String changedByName = resolveChangedBy(audit.getChangedBy());

        return new RestAuditLogDto(
                audit.getEntity().getClass().getSimpleName(),
                getEntityIdAsString(audit.getEntity()),
                audit.getRevisionType().name(),
                changedByName,
                formattedDate
        );
    }

    public List<RestAuditLogDto> toDtoList(List<AuditEntity<?>> audits) {
        return audits.stream().map(this::toDto).collect(Collectors.toList());
    }

    private String resolveChangedBy(Integer userId) {
        if (userId == null) return "System";
        try {
            User user = Context.getUserService().getUser(userId);
            if (user != null) {
                return user.getPerson() != null
                        ? user.getPerson().getPersonName().getFullName()
                        : user.getUsername();
            }
            return "User ID: " + userId;
        } catch (Exception e) {
            return "User ID: " + userId;
        }
    }

    private String getEntityIdAsString(Object entity) {
        try {
            return String.valueOf(entity.getClass().getMethod("getId").invoke(entity));
        } catch (Exception e) {
            try {
                return String.valueOf(entity.getClass().getMethod("getUuid").invoke(entity));
            } catch (Exception ex) {
                return "unknown";
            }
        }
    }
}
