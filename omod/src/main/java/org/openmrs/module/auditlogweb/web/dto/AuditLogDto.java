package org.openmrs.module.auditlogweb.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.envers.RevisionType;
import org.openmrs.GlobalProperty;
import org.openmrs.Role;

import java.util.Date;

@Data
@AllArgsConstructor
public class AuditLogDto {
    private Object entity;
    private RevisionType revisionType;
    private String changedBy;
    private Date changedOn;
    private Object revisionEntity;
    private String entityClassSimpleName;

    /**
     * Gets the appropriate identifier for the entity based on its type
     *
     * @return String representation of the entity's identifier
     */
    public String getEntityIdentifier() {
        if (entity == null) {
            return "NA";
        }

        if (entity instanceof Role) {
            return ((Role) entity).getRole();
        } else if (entity instanceof GlobalProperty) {
            return ((GlobalProperty) entity).getProperty();
        } else {
            try {
                return entity.getClass().getMethod("getId").invoke(entity).toString();
            } catch (Exception e) {
                return "NA";
            }
        }
    }
}