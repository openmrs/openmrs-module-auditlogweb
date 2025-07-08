/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.web;

import lombok.RequiredArgsConstructor;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.api.utils.EnversUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * UI helper component that provides admin-facing guidance related to audit logging configuration.
 * <p>
 * This class is intended to be used in the presentation layer (omod) to generate messages,
 * such as setup instructions, that should only be visible to users with system administrator privileges.
 */
@RequiredArgsConstructor
@Component
public class EnversUiHelper {

    private final MessageSource messageSource;

    /**
     * Returns an HTML-formatted hint for enabling audit logging, intended for users
     * with the "System Developer" role.
     * <p>
     * If the currently authenticated user has system administrator privileges, this method retrieves
     * a localized instructional message from the {@code messages.properties} file. Otherwise, it returns an empty string.
     *
     * @return a localized HTML hint string for system admins, or an empty string if the user is not authorized.
     */
    public String getAdminHint() {
        if (EnversUtils.isCurrentUserSystemAdmin()) {
            return messageSource.getMessage("auditlogweb.admin.hint", null, LocaleContextHolder.getLocale());
        }
        return "";
    }
}