/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory tracker for the password-reset flow, keyed by session ID.
 * Using this to carry the username/userId from the reset request step to the
 * actual password change step, and to mark the next login after a reset as non-manual.
 * 
 * Multiple requests can update/read the same session entry concurrently during reset flow.
 * Synchronized access on state objects prevents race conditions and stale reads.
 */
public final class PasswordResetFlowContext {

    private static final Map<String, PasswordResetFlowState> STATES = new ConcurrentHashMap<>();

    private PasswordResetFlowContext() {
    }

    public static void markResetRequest(String sessionId, String username, Integer userId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }

        PasswordResetFlowState state = STATES.computeIfAbsent(sessionId, key -> new PasswordResetFlowState());
        synchronized (state) {
            state.username = username;
            state.userId = userId;
            state.resetCompleted = false;
            state.isPasswordChangedBySystem = false;
        }
    }

    public static void markResetCompleted(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }

        PasswordResetFlowState state = STATES.computeIfAbsent(sessionId, key -> new PasswordResetFlowState());
        synchronized (state) {
            state.resetCompleted = true;
        }
        //Removing this because after the flow we don't need this and keep space free
        //Maybe we can delegate this for another separate function if needed the flow after password reset.
        STATES.remove(sessionId);
    }

    public static boolean hasPendingResetRequest(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }

        PasswordResetFlowState state = STATES.get(sessionId);
        if (state == null) {
            return false;
        }

        synchronized (state) {
            return !state.resetCompleted;
        }
    }

    public static boolean isPasswordChangedBySystem(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        PasswordResetFlowState state = STATES.get(sessionId);
        if (state == null) {
            return false;
        }
        synchronized (state) {
            return state.isPasswordChangedBySystem;
        }
    }

    public static void setPasswordChangedBySystem(String sessionId, boolean passwordChangedBySystem) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }

        PasswordResetFlowState state = STATES.get(sessionId);
        if (state == null) {
            return;
        }
        synchronized (state) {
            state.isPasswordChangedBySystem = passwordChangedBySystem;
        }
    }

    public static String resolveUsername(String sessionId) {
        PasswordResetFlowState state = STATES.get(sessionId);
        return state != null ? state.username : null;
    }

    public static Integer resolveUserId(String sessionId) {
        PasswordResetFlowState state = STATES.get(sessionId);
        return state != null ? state.userId : null;
    }

    private static final class PasswordResetFlowState {
        private String username;
        private Integer userId;
        private boolean resetCompleted;
        private boolean isPasswordChangedBySystem ;
    }
}