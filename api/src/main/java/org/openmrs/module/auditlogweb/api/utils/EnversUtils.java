package org.openmrs.module.auditlogweb.api.utils;

import org.openmrs.api.context.Context;

public class EnversUtils {
    public static boolean isEnversEnabled() {
        String value = Context.getRuntimeProperties().getProperty("hibernate.integration.envers.enabled");
               return Boolean.parseBoolean(value);
    }
}
