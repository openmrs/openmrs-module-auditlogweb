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

/**
 * Utility class for mapping {@link AuditEntity} objects to {@link RestAuditLogDto} DTOs.
 * Handles formatting of revision date and resolving the username of the user who made the change.
 */
@Component
public class AuditLogMapper {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z")
            .withZone(ZoneId.of("GMT"));

    /**
     * Converts a single {@link AuditEntity} to {@link RestAuditLogDto}.
     *
     * @param audit the audit entity to convert
     * @return the mapped {@link RestAuditLogDto} object
     */
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

    /**
     * Converts a list of {@link AuditEntity} objects to a list of {@link RestAuditLogDto} objects.
     *
     * @param audits list of audit entities
     * @return list of mapped DTOs
     */
    public List<RestAuditLogDto> toDtoList(List<AuditEntity<?>> audits) {
        return audits.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Resolves the username of the user who made the change.
     * Returns "System" if userId is null, or the full name/username if found.
     *
     * @param userId ID of the user who made the change
     * @return resolved username as a String
     */
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

    /**
     * Attempts to get the ID of the entity as a String.
     * Falls back to UUID or "unknown" if ID cannot be retrieved.
     *
     * @param entity the entity object
     * @return entity ID as String
     */
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
