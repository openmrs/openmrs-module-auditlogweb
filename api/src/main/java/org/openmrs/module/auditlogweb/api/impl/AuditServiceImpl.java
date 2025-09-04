/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.GlobalProperty;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.dao.AuditDao;
import org.openmrs.module.auditlogweb.api.dto.AuditFieldDiff;
import org.openmrs.module.auditlogweb.api.dto.AuditLogDetailDTO;
import org.openmrs.module.auditlogweb.api.utils.AuditTypeMapper;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.util.stream.Collectors;


/**
 * Default implementation of the {@link AuditService} interface.
 *
 * <p>This service delegates actual data retrieval to the {@link AuditDao} layer,
 * and provides fallback logic for resolving user details and filtering audits
 * by user or date range.
 */
@RequiredArgsConstructor
@Service
public class AuditServiceImpl extends BaseOpenmrsService implements AuditService {

    private final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);
    private final AuditDao auditDao;

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<AuditEntity<T>> getAllRevisions(Class<T> entityClass, int page, int size, String sortOrder) {
        return auditDao.getAllRevisions(entityClass, page, size, sortOrder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<AuditEntity<T>> getAllRevisions(String entityClassName, int page, int size, String sortOrder) {
        try {
            Class<T> clazz = (Class<T>) Class.forName(entityClassName);
            return getAllRevisions(clazz, page, size, sortOrder);
        } catch (ClassNotFoundException e) {
            log.error("Entity class not found: {}", entityClassName, e);
            return new ArrayList<>();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getRevisionById(Class<T> entityClass, Object entityId, int revisionId) {
        try {
            if (entityId instanceof Integer) {
                return auditDao.getRevisionById(entityClass, entityId, revisionId);
            } else if (entityId instanceof String) {
                if (Role.class.isAssignableFrom(entityClass)) {
                    return (T) auditDao.getRoleRevisionById((String) entityId, revisionId);
                } else if (GlobalProperty.class.isAssignableFrom(entityClass)) {
                    return (T) auditDao.getGlobalPropertyRevisionById((String) entityId, revisionId);
                } else {
                    return null;
                }
            }
        } catch (org.hibernate.ObjectNotFoundException e) {
            log.warn("Revision not found for entity [{}] with ID [{}]", entityClass.getSimpleName(), entityId);
            return null;
        }
        throw new IllegalArgumentException("Unsupported ID type for entity: " + entityClass.getName());
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> AuditEntity<T> getAuditEntityRevisionById(Class<T> entityClass, Object id, int revisionId) {
        if (id instanceof Integer) {
            return auditDao.getAuditEntityRevisionById(entityClass, (Integer) id, revisionId);
        } else if (id instanceof String) {
            if (entityClass == Role.class) {
                return (AuditEntity<T>) auditDao.getRoleAuditEntityRevisionById((String) id, revisionId);
            } else if (entityClass == GlobalProperty.class) {
                return (AuditEntity<T>) auditDao.getGlobalPropertyAuditEntityRevisionById((String) id, revisionId);
            }
        }
        throw new IllegalArgumentException("Unsupported ID type for revision retrieval: " + id.getClass().getSimpleName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long countAllRevisions(Class<T> entityClass) {
        return auditDao.countAllRevisions(entityClass);
    }

    /**
     * Counts all revisions for a given class name. If the class cannot be loaded,
     * logs the error and returns 0.
     *
     * @param entityClassName fully qualified name of the entity class
     * @return the total number of revisions, or 0 if the class is not found
     */
    public long countAllRevisions(String entityClassName) {
        try {
            Class<?> clazz = Class.forName(entityClassName);
            return countAllRevisions(clazz);
        } catch (ClassNotFoundException e) {
            log.error("Entity class not found: {}", entityClassName, e);
            return 0L;
        }
    }

    /**
     * Resolves a user ID to a displayable username.
     * Falls back to the user's system ID if the username is blank.
     * If the user or ID is not found, returns "Unknown".
     *
     * @param userId the OpenMRS user ID
     * @return the username, system ID, or "Unknown"
     */
    @Override
    public String resolveUsername(Integer userId) {
        if (userId == null) {
            return "Unknown";
        }

        User user = Context.getUserService().getUser(userId);
        if (user == null) {
            return "Unknown";
        }

        String username = user.getDisplayString();
        if (StringUtils.isBlank(username)) {
            return StringUtils.defaultIfBlank(user.getSystemId(), "Unknown");
        }
        return username;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<AuditEntity<T>> getRevisionsWithFilters(Class<T> clazz, int page, int size, Integer userId, Date startDate, Date endDate, String sortOrder) {
        return auditDao.getRevisionsWithFilters(clazz, page, size, userId, startDate, endDate, sortOrder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long countRevisionsWithFilters(Class<T> clazz, Integer userId, Date startDate, Date endDate) {
        return auditDao.countRevisionsWithFilters(clazz, userId, startDate, endDate);
    }

    /**
     * Resolves a user's ID based on their username or full name using OpenMRS's
     * partial match functionality. Returns {@code null} if no match is found.
     *
     * @param input the username or full name of the user
     * @return the user's ID, or {@code null} if not found
     */
    @Override
    public Integer resolveUserId(String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }

        List<User> matchedUsers = Context.getUserService().getUsers(input, null, false);
        if (!matchedUsers.isEmpty()) {
            return matchedUsers.get(0).getUserId();
        }

        return null;
    }
    /**
     * Retrieves a paginated list of audit entries across all Envers-audited entity types.
     *
     * @param page       the page number (0-based)
     * @param size       the number of records per page
     * @param userId     optional user ID to filter by the user who made the change
     * @param startDate  optional start date to filter changes from
     * @param endDate    optional end date to filter changes up to
     * @return a paginated list of {@link AuditEntity} objects across all audited entities
     */
    @Override
    public List<AuditEntity<?>> getAllRevisionsAcrossEntities(int page, int size, Integer userId, Date startDate, Date endDate, String sortOrder) {
        return auditDao.getAllRevisionsAcrossEntities(page, size, userId, startDate, endDate, sortOrder);
    }

    /**
     * Counts the total number of audit entries across all Envers-audited entity types,
     * filtered optionally by user and date range.
     *
     * @param userId     optional user ID to filter by the user who made the change
     * @param startDate  optional start date to filter changes from
     * @param endDate    optional end date to filter changes up to
     * @return the total number of matching audit entries
     */
    @Override
    public long countRevisionsAcrossEntities(Integer userId, Date startDate, Date endDate) {
        return auditDao.countRevisionsAcrossEntities(userId, startDate, endDate);
    }

    /**
     * Returns the total count of audit log entries across all audited entities.
     *
     * <p>This is used primarily for pagination metadata in REST responses.
     *
     * @return the total number of audit log entries
     */
    @Override
    public long getAuditLogsCount() {
        return auditDao.countRevisionsAcrossEntities(null, null, null);
    }

    /**
     * Counts the total number of audit log entries matching the given filters.
     * <p>
     * If a start date is provided without an end date, the current date is used as the end date.
     * If the end date is before the start date, zero is returned.
     *
     * @param userId     optional filter for the user ID who made the changes; can be null
     * @param startDate  optional filter for the start of the date range; can be null
     * @param endDate    optional filter for the end of the date range; can be null
     * @param entityType optional filter for the type of entity (e.g., "Patient", "Order"); can be null
     * @return the total count of audit log entries matching the filters
     */
    @Override
    public long getAuditLogsCount(Integer userId, Date startDate, Date endDate, String entityType) {
        if (startDate != null && endDate == null) {
            endDate = new Date();
        }
        if (startDate != null && endDate.before(startDate)) {
            return 0L;
        }
        return auditDao.countRevisionsAcrossEntities(userId, startDate, endDate, entityType);
    }
    /**
     * Maps a list of {@link AuditEntity} objects to their corresponding {@link AuditLogDetailDTO} representations.
     * Each DTO contains information about the revision, user, and changed fields.
     *
     * @param auditEntities the list of audit entities to map
     * @return a list of audit log detail DTOs representing the changes
     */
    @Override
    public List<AuditLogDetailDTO> mapAuditEntitiesToDetails(List<AuditEntity<?>> auditEntities) {
        List<AuditLogDetailDTO> dtoList = new ArrayList<>();

        for (AuditEntity<?> entity : auditEntities) {
            Object currentEntity = entity.getEntity();
            Object oldEntity = fetchPreviousRevision(entity, currentEntity);

            List<AuditFieldDiff> changedFields = extractChangedFields(currentEntity, oldEntity);

            AuditLogDetailDTO dto = buildAuditLogDetailDTO(entity, currentEntity, changedFields);
            dtoList.add(dto);
        }

        return dtoList;
    }
    private Object fetchPreviousRevision(AuditEntity<?> entity, Object currentEntity) {
        if (entity.getRevisionEntity().getId() <= 1) {
            return null;
        }

        Object entityId = UtilClass.getEntityIdAsString(currentEntity);
        try {
            return getRevisionById(
                    currentEntity.getClass(),
                    entityId,
                    entity.getRevisionEntity().getId() - 1
            );
        } catch (IllegalArgumentException e) {
            log.warn("Previous revision not supported for entity [{}] with ID [{}]",
                    currentEntity.getClass().getSimpleName(), entityId);
            return null;
        }
    }
    private List<AuditFieldDiff> extractChangedFields(Object currentEntity, Object oldEntity) {
        List<AuditFieldDiff> diffs = UtilClass.computeFieldDiffs(
                currentEntity.getClass(), oldEntity, currentEntity
        );

        return diffs.stream()
                .filter(AuditFieldDiff::isChanged)
                .map(d -> {
                    AuditFieldDiff f = new AuditFieldDiff();
                    f.setFieldName(d.getFieldName());
                    f.setOldValue(d.getOldValue());
                    f.setCurrentValue(d.getCurrentValue());
                    f.setChanged(true);
                    return f;
                })
                .collect(Collectors.toList());
    }
    
    private AuditLogDetailDTO buildAuditLogDetailDTO(
            AuditEntity<?> entity, Object currentEntity, List<AuditFieldDiff> changedFields) {

        String auditType = AuditTypeMapper.toHumanReadable(entity.getRevisionType());
        String username = resolveUsername(entity.getChangedBy());

        AuditLogDetailDTO dto = new AuditLogDetailDTO();
        dto.setRevisionID(entity.getRevisionEntity().getId());
        dto.setEntityType(currentEntity.getClass().getSimpleName());
        dto.setEventType(auditType);
        dto.setChangedBy(username);
        dto.setChangedOn(entity.getRevisionEntity().getChangedOn());
        dto.setChanges(changedFields);

        return dto;
    }

}
