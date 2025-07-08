/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.utils;

import org.hibernate.envers.RevisionType;

/**
 * Utility class to convert Hibernate Envers {@link RevisionType} values
 * to human-readable audit type descriptions.
 */
public class AuditTypeMapper {

    /**
     * Converts a RevisionType to a descriptive string.
     *
     * @param revisionType the revision type
     * @return a human-readable string like "Record was added", etc.
     */
    public static String toHumanReadable(RevisionType revisionType) {
        if (revisionType == null) {
            return "Unknown change type";
        }

        switch (revisionType) {
            case ADD: return "Record was added";
            case MOD: return "Record was modified";
            case DEL: return "Record was deleted";
            default: return "Unknown change type";
        }
    }
}