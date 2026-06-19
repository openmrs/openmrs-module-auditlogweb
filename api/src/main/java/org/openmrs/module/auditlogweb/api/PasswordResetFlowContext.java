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
 * It tracks the complete flow of password reset request
 * 
 * Multiple requests can update/read the same session entry concurrently during reset flow.
 * That's why synchronized access on state objects prevents race conditions and stale reads.
 */
public final class PasswordResetFlowContext {

    private static final Map<String, PasswordResetFlowState> STATES = new ConcurrentHashMap<>();

    
    /**
     * Starts the password reset flow and marks that a password reset has been requested for the given session.
     *
     * @param sessionId the session identifier for the password reset flow
     */
    public static void markResetRequest(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }

        PasswordResetFlowState state = STATES.computeIfAbsent(sessionId, key -> new PasswordResetFlowState());
        synchronized (state) {
            state.resetCompleted = false;
            state.isPasswordChangedBySystem = false;
        }
    }

    /**
     * Marks the password reset flow for the given session as completed by removing its state.
     *
     * @param sessionId the session identifier for the password reset flow
     */
    public static void markResetCompleted(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }
        STATES.remove(sessionId);
    }

    /**
     * Clears and reclaims any tracked password reset state for the given session ID.
     *
     * @param sessionId the session identifier to clear
     */
    public static void clear(String sessionId) {
        markResetCompleted(sessionId);
    }

    /**
     * Checks whether there is a pending(not yet completed) password reset request for the given session.
     * Returns false for null/empty session IDs or when no state is found.
     *
     * @param sessionId the session identifier to check
     * @return true when a reset request exists and has not been completed
     */
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

    /**
     * Tells whether the password for the given session has changed by the system as part of the reset flow .
     * Returns false for null/empty session IDs or when no state exists.
     *
     * @param sessionId the session identifier to query
     * @return true if the password was changed by the system, false otherwise
     */
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

    /**
     * Marks whether the password has changed by the system for the given password reset session.
     *
     * @param sessionId the password reset session ID
     * @param passwordChangedBySystem true if changed by system
     */
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


    private static final class PasswordResetFlowState {

        private boolean resetCompleted;
        private boolean isPasswordChangedBySystem;
    }
}
