/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.AuditSecurityEvent;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;
import org.openmrs.test.jupiter.BaseContextSensitiveTest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PasswordAuditAdviceIntegrationTest extends BaseContextSensitiveTest {

    private UserService userService;
    private AuditService auditService;
    private User testUser;

    @BeforeEach
    public void setup() throws Exception {
        userService = Context.getUserService();
        auditService = Context.getService(AuditService.class);
        testUser = userService.getUserByUsername("admin");
        userService.changeQuestionAnswer(testUser, "What is your favorite color?", "blue");
    }

    @Test
    public void shouldLogPasswordResetRequestSuccessAndPasswordResetSuccessInRealFlow() throws Exception {
        int initialCount = auditService.getSecurityEvents(null, null, null, null, 0, 100).size();

        AuditLogContext ctx = new AuditLogContext();
        ctx.setSessionId("test-session-1");
        AuditLogContext.set(ctx);

        boolean answerMatches = userService.isSecretAnswer(testUser, "blue");
        assertTrue(answerMatches);

        userService.changePasswordUsingSecretAnswer("blue", "NewPassword123!");
        List<AuditSecurityEvent> events = auditService.getSecurityEvents(null, null, null, null, 0, 100);
        int finalCount = events.size();

        assertTrue(finalCount - initialCount >= 2);

        boolean foundRequestSuccess = false;
        boolean foundResetSuccess = false;

        for (AuditSecurityEvent event : events) {
            if (event.getEventType() == AuditSecurityEventType.PASSWORD_RESET_REQUEST_SUCCESS && "admin".equals(event.getUsername())) {
                foundRequestSuccess = true;
            }
            if (event.getEventType() == AuditSecurityEventType.PASSWORD_RESET_SUCCESS && "admin".equals(event.getUsername())) {
                foundResetSuccess = true;
            }
        }

        assertTrue(foundRequestSuccess);
        assertTrue(foundResetSuccess);

        AuditLogContext.clear();
    }

    @Test
    public void shouldLogPasswordResetRequestFailureInRealFlow() throws Exception {
        int initialCount = auditService.getSecurityEvents(null, null, null, null, 0, 100).size();

        AuditLogContext ctx = new AuditLogContext();
        ctx.setSessionId("test-session-2");
        AuditLogContext.set(ctx);

        boolean answerMatches = userService.isSecretAnswer(testUser, "wrong-answer");
        assertFalse(answerMatches);

        List<AuditSecurityEvent> events = auditService.getSecurityEvents(null, null, null, null, 0, 100);
        int finalCount = events.size();

        assertTrue(finalCount - initialCount >= 1);
        boolean foundRequestFailure = false;

        for (AuditSecurityEvent event : events) {
            if (event.getEventType() == AuditSecurityEventType.PASSWORD_RESET_REQUEST_FAILURE && "admin".equals(event.getUsername())) {
                foundRequestFailure = true;
            }
        }

        assertTrue(foundRequestFailure);
        AuditLogContext.clear();
    }

    @Test
    public void shouldLogPasswordResetFailureWhenActivationKeyIsInvalid() throws Exception {
        int initialCount = auditService.getSecurityEvents(null, null, null, null, 0, 100).size();

        AuditLogContext ctx = new AuditLogContext();
        ctx.setSessionId("test-session-3");
        AuditLogContext.set(ctx);

        try {
            userService.changePasswordUsingActivationKey("invalid-key", "NewPassword123!");
        } catch (Exception e) {
            // Here exception will come because key is invalid
        }

        List<AuditSecurityEvent> events = auditService.getSecurityEvents(null, null, null, null, 0, 100);
        int finalCount = events.size();

        assertTrue(finalCount - initialCount >= 1);
        boolean foundResetFailure = false;

        for (AuditSecurityEvent event : events) {
            if (event.getEventType() == AuditSecurityEventType.PASSWORD_RESET_FAILURE) {
                foundResetFailure = true;
            }
        }

        assertTrue(foundResetFailure);
        AuditLogContext.clear();
    }
}
