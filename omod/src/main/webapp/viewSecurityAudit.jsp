<%--
  This Source Code Form is subject to the terms of the Mozilla Public License,
  v. 2.0. If a copy of the MPL was not distributed with this file, You can
  obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
  the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
  Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
  graphic logo is a trademark of OpenMRS Inc.
--%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp"%>

<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@300;400;600&display=swap">
<link rel="stylesheet" type="text/css" href="<openmrs:contextPath/>/moduleResources/auditlogweb/css/viewSecurityAudit.css" />

<div class="security-audit-detail-container">

    <div class="title-section">
        <h1 class="detail-title">Security Audit Event Detail</h1>
        <p class="detail-subtitle">Viewing detailed record for security event</p>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="error-box">${errorMessage}</div>
    </c:if>

    <c:if test="${not empty event}">
        <div class="info-section">
            <h2 class="section-title">PRIMARY INFORMATION</h2>
            <table class="detail-table">
                <tbody>
                    <tr>
                        <td class="label-cell">Event ID</td>
                        <td>${event.id}</td>
                    </tr>
                    <tr>
                        <td class="label-cell">Event Type</td>
                        <td>
                            <c:choose>
                                <c:when test="${fn:contains(event.eventType,'LOGIN_SUCCESS')}">
                                     <span class="badge badge-login-success">${event.eventType}</span>
                                </c:when>
                                <c:when test="${fn:contains(event.eventType, 'PASSWORD') || fn:contains(event.eventType, 'LOGOUT')}">
                                    <span class="badge badge-logout">${event.eventType}</span>
                                </c:when>
                                <c:when test="${fn:contains(event.eventType, 'LOGIN_FAILURE') ||  fn:contains(event.eventType, 'ACCOUNT')}">
                                    <span class="badge badge-security">${event.eventType}</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge badge-password">${event.eventType}</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <tr>
                        <td class="label-cell">Username</td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty event.username}">${event.username}</c:when>
                                <c:otherwise><span class="null-value">-</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <tr>
                        <td class="label-cell">User ID</td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty event.userId}">${event.userId}</c:when>
                                <c:otherwise><span class="null-value">-</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <tr>
                        <td class="label-cell">Event Time</td>
                        <td><fmt:formatDate value="${event.eventTime}" pattern="yyyy-MM-dd HH:mm:ss" /></td>
                    </tr>
                    <tr>
                        <td class="label-cell">IP Address</td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty event.ipAddress}">${event.ipAddress}</c:when>
                                <c:otherwise><span class="null-value">-</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <tr>
                        <td class="label-cell">User Agent</td>
                        <td class="user-agent-cell">
                            <c:choose>
                                <c:when test="${not empty event.userAgent}">
                                    <span class="truncate-user-agent">${event.userAgent}</span>
                                </c:when>
                                <c:otherwise><span class="null-value">-</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <tr>
                        <td class="label-cell">Session ID</td>
                        <td class="session-id-cell">
                            <c:choose>
                                <c:when test="${not empty event.sessionId}">${event.sessionId}</c:when>
                                <c:otherwise><span class="null-value">-</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="info-section">
            <h2 class="section-title">DETAILS</h2>
            <div class="details-content">
                <c:choose>
                    <c:when test="${not empty event.details}">
                        <pre class="json-details"><c:out value="${event.details}"/></pre>
                    </c:when>
                    <c:otherwise>
                        <p class="no-details">No additional details available.</p>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Related Activity Section -->
        <c:if test="${not empty relatedEvents}">
            <div class="info-section">
                <h2 class="section-title">RELATED ACTIVITY (SAME SESSION)</h2>
                <table class="related-table">
                    <thead>
                        <tr>
                            <th>Time</th>
                            <th>Type</th>
                            <th>User</th>
                            <th>Details</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="relEvent" items="${relatedEvents}">
                            <tr <c:if test="${relEvent.id == event.id}">class="current-event"</c:if>>
                                <td><fmt:formatDate value="${relEvent.eventTime}" pattern="HH:mm:ss" /></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${fn:contains(relEvent.eventType,'LOGIN_SUCCESS')}">
                                             <span class="badge badge-login-success">${relEvent.eventType}</span>
                                        </c:when>
                                        <c:when test="${fn:contains(relEvent.eventType, 'PASSWORD') || fn:contains(relEvent.eventType, 'LOGOUT')}">
                                            <span class="badge badge-logout">${relEvent.eventType}</span>
                                        </c:when>
                                        <c:when test="${fn:contains(relEvent.eventType, 'LOGIN_FAILURE') ||  fn:contains(relEvent.eventType, 'ACCOUNT')}">
                                            <span class="badge badge-security">${relEvent.eventType}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-password">${relEvent.eventType}</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty relEvent.username}">${relEvent.username}</c:when>
                                        <c:otherwise><span class="null-value">-</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:set var="ipText" value="${empty relEvent.ipAddress ? '-' : relEvent.ipAddress}"/>
                                    <c:set var="detailsText" value="IP: ${ipText}"/>
                                    <c:choose>
                                        <c:when test="${relEvent.id == event.id}"><strong>This Event</strong></c:when>
                                        <c:otherwise>${detailsText}</c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:if>

        <div class="footer-actions">
            <a href="${pageContext.request.contextPath}/module/auditlogweb/securityauditlogs.form" class="btn btn-secondary">
                Back to Audit Logs
            </a>
        </div>

    </c:if>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>
