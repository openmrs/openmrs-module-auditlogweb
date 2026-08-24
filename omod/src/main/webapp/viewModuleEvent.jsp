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
<link rel="stylesheet" type="text/css" href="<openmrs:contextPath/>/moduleResources/auditlogweb/css/viewAuditLogDetail.css" />

<div class="audit-detail-container">

    <div class="title-section">
        <h1 class="detail-title">Module Event Audit Detail</h1>
        <p class="detail-subtitle">Viewing detailed record for module event</p>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="error-box"><c:out value="${errorMessage}"/></div>
    </c:if>

    <c:if test="${not empty event}">
        <div class="info-section">
            <h2 class="section-title">PRIMARY INFORMATION</h2>
            <table class="detail-table">
                <tbody>
                <tr>
                    <td class="label-cell">Event ID</td>
                    <td><c:out value="${event.id}"/></td>
                </tr>
                <tr>
                    <td class="label-cell">Event Type</td>
                    <td>
                        <c:choose>
                            <c:when test="${event.eventSuccess}">
                                <span class="badge badge-success"><c:out value="${event.eventType}"/></span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge badge-failure"><c:out value="${event.eventType}"/></span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
                <tr>
                    <td class="label-cell">Module Name</td>
                    <td>
                        <c:out value="${event.moduleName}"/>
                    </td>
                </tr>
                <tr>
                    <td class="label-cell">Module Version</td>
                    <td>
                        <c:out value="${event.moduleVersion}"/>
                    </td>
                </tr>
                <tr>
                    <td class="label-cell">Success</td>
                    <td>
                        <c:choose>
                            <c:when test="${event.eventSuccess}">
                                <span>Yes</span>
                            </c:when>
                            <c:otherwise>
                                <span>No</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
                <tr>
                    <td class="label-cell">Failure Reason</td>
                    <td>
                        <c:choose>
                            <c:when test="${not empty event.failureReason}"><c:out value="${event.failureReason}"/></c:when>
                            <c:otherwise><span class="null-value">-</span></c:otherwise>
                        </c:choose>
                    </td>
                </tr>
                <tr>
                    <td class="label-cell">Username</td>
                    <td>
                        <c:choose>
                            <c:when test="${not empty event.username}"><c:out value="${event.username}"/></c:when>
                            <c:otherwise><span class="null-value">-</span></c:otherwise>
                        </c:choose>
                    </td>
                </tr>
                <tr>
                    <td class="label-cell">User UUID</td>
                    <td>
                        <c:choose>
                            <c:when test="${not empty event.userUUID}"><c:out value="${event.userUUID}"/></c:when>
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
                            <c:when test="${not empty event.ipAddress}"><c:out value="${event.ipAddress}"/></c:when>
                            <c:otherwise><span class="null-value">-</span></c:otherwise>
                        </c:choose>
                    </td>
                </tr>
                <tr>
                    <td class="label-cell">User Agent</td>
                    <td class="user-agent-cell">
                        <c:choose>
                            <c:when test="${not empty event.userAgent}">
                                <span class="truncate-user-agent"><c:out value="${event.userAgent}"/></span>
                            </c:when>
                            <c:otherwise><span class="null-value">-</span></c:otherwise>
                        </c:choose>
                    </td>
                </tr>
                <tr>
                    <td class="label-cell">Session ID</td>
                    <td class="session-id-cell">
                        <c:choose>
                            <c:when test="${not empty event.sessionId}"><c:out value="${event.sessionId}"/></c:when>
                            <c:otherwise><span class="null-value">-</span></c:otherwise>
                        </c:choose>
                    </td>
                </tr>
                </tbody>
            </table>
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
                        <th>Module</th>
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
                                    <c:when test="${relEvent.eventSuccess}">
                                        <span class="badge badge-success"><c:out value="${relEvent.eventType}"/></span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge badge-failure"><c:out value="${relEvent.eventType}"/></span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${not empty relEvent.moduleName}">
                                        <c:out value="${relEvent.moduleName}"/>
                                        <c:if test="${not empty relEvent.moduleVersion}">
                                            (<c:out value="${relEvent.moduleVersion}" />)
                                        </c:if>
                                    </c:when>
                                    <c:otherwise><span class="null-value">-</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${not empty relEvent.username}"><c:out value="${relEvent.username}"/></c:when>
                                    <c:otherwise><span class="null-value">-</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:set var="ipText" value="${empty relEvent.ipAddress ? '-' : relEvent.ipAddress}"/>
                                <c:set var="detailsText" value="IP: ${ipText}"/>
                                <c:choose>
                                    <c:when test="${relEvent.id == event.id}"><strong>This Event</strong></c:when>
                                    <c:otherwise><c:out value="${detailsText}"/></c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:if>

        <div class="footer-actions">
            <a href="<c:out value='${pageContext.request.contextPath}'/>/module/auditlogweb/moduleEvents.form" class="btn btn-secondary">
                Back to Audit Logs
            </a>
        </div>

    </c:if>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>
