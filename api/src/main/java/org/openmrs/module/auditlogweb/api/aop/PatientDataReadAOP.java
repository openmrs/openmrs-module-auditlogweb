/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.aop;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.openmrs.OpenmrsObject;
import org.openmrs.module.auditlogweb.AppCacheManager;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.ReadAuditEntityMetadata;
import org.openmrs.module.auditlogweb.ReadAuditWorker;
import org.openmrs.module.auditlogweb.api.AuditLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class PatientDataReadAOP {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	private final ReadAuditWorker readAuditWorker;
	
	private final AppCacheManager appCacheManager;
	
	@Around("execution(* org.openmrs.api.PatientService.getPatient*(..)) || "
	        + "execution(* org.openmrs.api.PatientService.getAllPatient*(..)) || "
	        + "execution(* org.openmrs.api.PatientService.getDuplicatePatient*(..)) || "
	        + "execution(* org.openmrs.api.PatientService.getAllerg*(..)) ")
	public Object auditPatientDataRead(ProceedingJoinPoint joinPoint) throws Throwable {
		
		if (AopUtils.isAopProxy(joinPoint.getTarget()))
			return joinPoint.proceed();

		String returnDataType = null;
		Method method = null;
		String username = null;
		String userUUID = null;
		String ipAddress = null;
		String userAgent = null;
		String sessionId = null;
		try {
			MethodSignature signature = (MethodSignature) joinPoint.getSignature();
			method = signature.getMethod();
			returnDataType = getMethodReturnDataType(method);
			
			AuditLogContext ctx = AuditLogContext.get();
			if (ctx != null) {
				username = ctx.getLoggedInUsername() != null ? ctx.getLoggedInUsername() : null;
				userUUID = ctx.getLoggedInUserUUID() != null ? ctx.getLoggedInUserUUID() : null;
				ipAddress = ctx.getIpAddress() != null ? ctx.getIpAddress() : null;
				userAgent = ctx.getUserAgent() != null ? ctx.getUserAgent() : null;
				sessionId = ctx.getSessionId() != null ? ctx.getSessionId() : null;
			}
		}
		catch (Exception e) {
			log.warn("Error while getting  data from audit context", e);
		}
		
		Object result = null;
		List<ReadAuditEntityMetadata> newTargetEntities = new ArrayList<>();
		boolean isReadSuccess = true;
		try {
			result = joinPoint.proceed();
			try {
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
			}
			catch (Exception e) {
				log.error("Error while getting read audit", e);
			}
			return result;
		}
		catch (Throwable e) {
			isReadSuccess = false;
			throw e;
		}
		finally {
			log.debug("Read patient AOP completed");
			if (!isReadSuccess || !newTargetEntities.isEmpty()) {
				try {
					ReadAuditLog readAuditLog = ReadAuditLog.builder().entityName(returnDataType).eventTime(new Date())
							.username(username).userUUID(userUUID).userAgent(userAgent).sessionId(sessionId)
							.ipAddress(ipAddress).isReadSuccess(isReadSuccess).build();
					readAuditLog.setTargets(newTargetEntities);
					readAuditWorker.submitTask(readAuditLog);
					log.debug("Submitted the Read Audit log to worker");
				}
				catch (Exception e) {
					log.error("Error while submitting read audit to worker ", e);
				}
			} else {
				log.debug("Skipping the read audit log save");
			}
		}
		
	}
	
	public String getMethodReturnDataType(Method method) {
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
