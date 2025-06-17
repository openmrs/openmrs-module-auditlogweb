/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.utils;

import org.openmrs.api.context.Context;

public class EnversUtils {
    public static boolean isEnversEnabled() {
        String value = Context.getRuntimeProperties().getProperty("hibernate.integration.envers.enabled");
        return Boolean.parseBoolean(value);
    }

    public static boolean isCurrentUserSystemAdmin() {
        return Context.isAuthenticated() && Context.getAuthenticatedUser().hasRole("System Developer");
    }

    public static String getAdminHint() {
        if (isCurrentUserSystemAdmin()) {
            return "As a System Administrator, you can enable audit logging by adding the following lines to your"
                    + " <code>openmrs-runtime.properties</code> file:"
                    + "<br/><code>hibernate.integration.envers.enabled=true</code>"
                    + "<br/><code>hibernate.hbm2ddl.auto=update</code>";
        }
        return "";
    }
}
