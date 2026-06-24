package org.openmrs.module.auditlogweb.api.aop;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.openmrs.OpenmrsObject;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.ReadAuditEntityMetadata;
import org.openmrs.module.auditlogweb.api.ReadAuditService;
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
    private final ReadAuditService readAuditService;

    @Around("execution(* org.openmrs.api.PatientService.getPatient*(..)) || "
            + "execution(* org.openmrs.api.PatientService.getAllPatient*(..)) || "
            + "execution(* org.openmrs.api.PatientService.getDuplicatePatient*(..)) || "
            + "execution(* org.openmrs.api.PatientService.getAllerg*(..)) ")
    public Object auditPatientDataRead(ProceedingJoinPoint joinPoint) throws Throwable {

        if (AopUtils.isAopProxy(joinPoint.getTarget())){
            return joinPoint.proceed();
        };

        ReadAuditLog auditData = new ReadAuditLog();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        log.info("Read patient AOP called for {}", method.getName());

        String returnDataType = getMethodReturnDataType(method);
        auditData.setEntityName(returnDataType);
        auditData.setEventTime(new Date());
        auditData.setUsername("admin");
        auditData.setIpAddress("127.0.0.1");

        Object result = null;
        try {
            result = joinPoint.proceed();
            try {
                auditData.setReadSuccess(true);
                List<ReadAuditEntityMetadata> targets = getEntityMetadata(result);
                for (ReadAuditEntityMetadata target : targets) {
                    target.setReadAuditLog(auditData);
                }
                auditData.setTargets(targets);
            }catch(Exception e) {
                log.error("Error while getting read audit",e);
            }
            log.info("Read patient AOP completed");
            return result;
        } catch (Throwable e) {
            auditData.setReadSuccess(false);
            throw e;
        } finally{
            log.info("Saved the Read Audit log");
            try {
                readAuditService.logReadAudit(auditData);
            }catch(Exception e) {
                log.error("Error while saving read audit ",e);
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
            ReadAuditEntityMetadata entity = new ReadAuditEntityMetadata();
            entity.setEntityUuid(openmrsObject.getUuid());
            return entity;
        }
        return null;
    }

}