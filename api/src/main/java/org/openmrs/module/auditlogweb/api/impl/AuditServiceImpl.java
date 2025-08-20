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
import org.openmrs.module.auditlogweb.api.dto.RestAuditLogDto;
import org.openmrs.module.auditlogweb.api.utils.AuditLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;


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
    private final AuditLogMapper dtoMapper;

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
        if (entityId instanceof Integer) {
            return auditDao.getRevisionById(entityClass, (Integer) entityId, revisionId);
        } else if (entityId instanceof String) {
            // Handle string IDs for Role and GlobalProperty
            if (Role.class.isAssignableFrom(entityClass)) {
                return (T) auditDao.getRoleRevisionById((String) entityId, revisionId);
            } else if (GlobalProperty.class.isAssignableFrom(entityClass)) {
                return (T) auditDao.getGlobalPropertyRevisionById((String) entityId, revisionId);
            }
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
     * Converts audit entries from multiple entity types into REST-friendly DTOs
     * and returns a paginated list sorted by revision date (default: descending).
     *
     * <p>Each DTO contains entity type, ID, revision type, changedBy info, and
     * formatted revision date in GMT.
     *
     * @param page the page number (zero-based)
     * @param size the number of records per page
     * @return a list of {@link RestAuditLogDto} representing audit logs across entities
     */
    @Override
    public List<RestAuditLogDto> getAllAuditLogs(int page, int size) {
        List<AuditEntity<?>> audits = auditDao.getAllRevisionsAcrossEntities(page, size, null, null, null, "desc");
        return dtoMapper.toDtoList(audits);
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

    @Override
    public List<RestAuditLogDto> getAllAuditLogs(int page, int size, Integer userId, Date startDate, Date endDate, String entityType) {
        if (startDate != null && endDate == null) {
            endDate = new Date();
        }
        if (startDate != null && endDate.before(startDate)) {
            return Collections.emptyList();
        }

        List<AuditEntity<?>> audits = auditDao.getAllRevisionsAcrossEntities(page, size, userId, startDate, endDate, "desc", entityType);

        // Use the mapper instead of inline stream mapping
        return dtoMapper.toDtoList(audits);
    }

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

    private String formatRevisionDate(Object revisionEntity) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z")
                .withZone(ZoneId.of("GMT"));
        try {
            return formatter.format(((java.util.Date) revisionEntity.getClass()
                    .getMethod("getRevisionDate").invoke(revisionEntity)).toInstant());
        } catch (Exception e1) {
            try {
                long ts = (long) revisionEntity.getClass()
                        .getMethod("getTimestamp").invoke(revisionEntity);
                return formatter.format(Instant.ofEpochMilli(ts));
            } catch (Exception e2) {
                return "unknown";
            }
        }
    }

    private String getEntityIdAsString(Object entity) {
        try {
            Method getIdMethod = entity.getClass().getMethod("getId");
            return String.valueOf(getIdMethod.invoke(entity));
        } catch (Exception e) {
            try {
                Method getUuidMethod = entity.getClass().getMethod("getUuid");
                return String.valueOf(getUuidMethod.invoke(entity));
            } catch (Exception ex) {
                return "unknown";
            }
        }
    }

}
