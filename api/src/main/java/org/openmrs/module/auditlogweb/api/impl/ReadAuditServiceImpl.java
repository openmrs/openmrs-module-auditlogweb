/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.impl;

import lombok.RequiredArgsConstructor;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.auditlogweb.AppCacheManager;
import org.openmrs.module.auditlogweb.ReadAuditEntityMetadata;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.ReadAuditWorker;
import org.openmrs.module.auditlogweb.api.AuditLogContext;
import org.openmrs.module.auditlogweb.api.ReadAuditService;
import org.openmrs.module.auditlogweb.api.dao.ReadAuditDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
public class ReadAuditServiceImpl extends BaseOpenmrsService implements ReadAuditService {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	private final ReadAuditDAO readAuditDAO;
	
	@Autowired
	private AppCacheManager appCacheManager;
	
	@Autowired
	@Lazy
	private ReadAuditWorker readAuditWorker;
	
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void logReadAudit(ReadAuditLog readAuditLog) {
		readAuditDAO.saveReadAuditLog(readAuditLog);
	}
	
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void logReadAudits(List<ReadAuditLog> readAuditLogs) {
		if (readAuditLogs != null) {
			for (ReadAuditLog log : readAuditLogs) {
				readAuditDAO.saveReadAuditLog(log);
			}
		}
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<ReadAuditLog> getReadAuditLogs(String eventType, String username, Date startDate, Date endDate, int page,
	        int size) {
		return readAuditDAO.getReadAuditLogs(eventType, username, startDate, endDate, page, size);
	}
	
	@Override
	@Transactional(readOnly = true)
	public long countReadAuditLogs(String eventType, String username, Date startDate, Date endDate) {
		return readAuditDAO.countReadAuditLogs(eventType, username, startDate, endDate);
	}
	
	@Override
	public ReadAuditLog getReadAuditLogById(Integer id) {
		return readAuditDAO.getReadAuditLogById(id);
	}
	
	@Override
	public List<ReadAuditLog> getRelatedReadLogs(String sessionId, int limit) {
		return readAuditDAO.getRelatedReadLogs(sessionId, limit);
	}
	
	@Override
	public Object auditReadRequest(ProceedingJoinPoint joinPoint) throws Throwable {
		if (AopUtils.isAopProxy(joinPoint.getTarget())) {
			return joinPoint.proceed();
		}
		
		String returnDataType = null;
		Object result = null;
		boolean isReadSuccess = true;
		
		try {
			MethodSignature signature = (MethodSignature) joinPoint.getSignature();
			Method method = signature.getMethod();
			returnDataType = getMethodReturnDataType(method);
		}
		catch (Exception e) {
			log.warn("Error while getting data from audit context", e);
		}
		
		try {
			result = joinPoint.proceed();
			return result;
		}
		catch (Throwable e) {
			isReadSuccess = false;
			throw e;
		}
		finally {
			try {
				saveReadAuditRequest(returnDataType, isReadSuccess, result);
			}
			catch (Exception e) {
				log.warn("Error while saving read audit request", e);
			}
		}
	}
	
	@Override
	public void saveReadAuditRequest(String entityType, boolean isReadSuccess, Object result) {
		try {
			String username = null;
			String userUUID = null;
			String ipAddress = null;
			String userAgent = null;
			String sessionId = null;
			
			AuditLogContext ctx = AuditLogContext.get();
			if (ctx != null) {
				username = ctx.getLoggedInUsername();
				userUUID = ctx.getLoggedInUserUUID();
				ipAddress = ctx.getIpAddress();
				userAgent = ctx.getUserAgent();
				sessionId = ctx.getSessionId();
			}
			
			List<ReadAuditEntityMetadata> newTargetEntities = new ArrayList<>();
			List<ReadAuditEntityMetadata> targetEntities = getEntityMetadata(result);
			for (ReadAuditEntityMetadata targetEntity : targetEntities) {
				if (targetEntity.getEntityUuid() != null) {
					String userKey = username != null ? username : (userUUID != null ? userUUID : "anonymous");
					String safeIp = ipAddress != null ? ipAddress : "unknown";
					String key = userKey + ":" + safeIp + ":" + targetEntity.getEntityUuid();
					if (appCacheManager.get(key) == null) {
						appCacheManager.set(key, true);
						newTargetEntities.add(targetEntity);
					}
				}
			}
			
			if (!isReadSuccess || !newTargetEntities.isEmpty()) {
				ReadAuditLog readAuditLog = ReadAuditLog.builder().entityName(entityType).eventTime(new Date())
				        .username(username).userUUID(userUUID).userAgent(userAgent).sessionId(sessionId).ipAddress(ipAddress)
				        .isReadSuccess(isReadSuccess).build();
				readAuditLog.setTargets(newTargetEntities);
				readAuditWorker.submitTask(readAuditLog);
				log.debug("Submitted the Read Audit log to worker");
			}
		}
		catch (Exception e) {
			log.error("Error while saving read audit request", e);
		}
	}
	
	private String getMethodReturnDataType(Method method) {
		Class<?> returnType = method.getReturnType();
		if (Collection.class.isAssignableFrom(returnType)) {
			Type genericReturnType = method.getGenericReturnType();
			if (genericReturnType instanceof ParameterizedType) {
				ParameterizedType type = (ParameterizedType) genericReturnType;
				Type[] typeArguments = type.getActualTypeArguments();
				if (typeArguments.length > 0) {
					Type targetType = typeArguments[0];
					if (targetType instanceof Class) {
						return ((Class<?>) targetType).getSimpleName();
					}
				}
			}
		}
		return returnType.getSimpleName();
	}
	
	public List<ReadAuditEntityMetadata> getEntityMetadata(Object result) {
		List<ReadAuditEntityMetadata> auditLogEntities = new ArrayList<>();
		if (result == null) {
			return auditLogEntities;
		}
		if (result instanceof Collection) {
			for (Object obj : (Collection<?>) result) {
				if (obj instanceof OpenmrsObject) {
					ReadAuditEntityMetadata entity = createReadAuditLogEntity((OpenmrsObject) obj);
					if (entity != null) {
						auditLogEntities.add(entity);
					}
				}
			}
		} else if (result instanceof OpenmrsObject) {
			ReadAuditEntityMetadata entity = createReadAuditLogEntity((OpenmrsObject) result);
			if (entity != null) {
				auditLogEntities.add(entity);
			}
		}
		return auditLogEntities;
	}
	
	private ReadAuditEntityMetadata createReadAuditLogEntity(OpenmrsObject openmrsObject) {
		if (openmrsObject.getId() != null && openmrsObject.getUuid() != null) {
			return ReadAuditEntityMetadata.builder().entityUuid(openmrsObject.getUuid()).build();
		}
		return null;
	}
}
