<%--
  This Source Code Form is subject to the terms of the Mozilla Public License,
  v. 2.0. If a copy of the MPL was not distributed with this file, You can
  obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
  the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
  Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
  graphic logo is a trademark of OpenMRS Inc.
--%>
<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp"%>

<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@300;400;600&display=swap">
<link rel="stylesheet" type="text/css" href="<openmrs:contextPath/>/moduleResources/auditlogweb/css/securityauditlogs.css" />


<div class="security-page">
    <h1 class="security-title">Security Audit Logs Explorer</h1>
    <p class="security-subtitle">Review and filter the security audit records for compliance and monitoring.</p>

    <form id="securityAuditForm" action="securityauditlogs.form" method="get" autocomplete="off">
        <input type="hidden" id="pageInput" name="page" value="<c:out value='${currentPage != null ? currentPage : 0}'/>"/>

        <div class="security-filter-panel">
            <div>
                <label for="eventType">LOG TYPE</label>
                <select id="eventType" name="eventType">
                    <option value="">All</option>
                    <c:forEach var="type" items="${eventTypes}">
                        <option value="<c:out value='${type}'/>" <c:if test="${eventType == type}">selected</c:if>><c:out value="${type}"/></option>
                    </c:forEach>
                </select>
            </div>

            <div>
                <label for="username">USER</label>
                <input type="text" id="username" name="username" value="<c:out value='${usernameFilter}'/>" placeholder="Search users..."/>
            </div>

            <div>
                <label for="startDate">FROM DATE</label>
                <input type="date" id="startDate" name="startDate" value="<c:out value='${startDate}'/>"/>
            </div>

            <div>
                <label for="endDate">TO DATE</label>
                <input type="date" id="endDate" name="endDate" value="<c:out value='${endDate}'/>"/>
            </div>

            <div>
                <label for="size">ITEMS</label>
                <select id="size" name="size">
                    <option value="15" <c:if test="${pageSize == 15}">selected</c:if>>15</option>
                    <option value="25" <c:if test="${pageSize == 25}">selected</c:if>>25</option>
                    <option value="50" <c:if test="${pageSize == 50}">selected</c:if>>50</option>
                </select>
            </div>

            <div>
                <label>&nbsp;</label>
                <button type="submit">Search</button>
            </div>
        </div>
    </form>

    <c:if test="${not empty errorMessage}">
        <div class="error-box"><c:out value="${errorMessage}"/></div>
    </c:if>

    <table class="security-table">
        <thead>
        <tr>
            <th>Timestamp</th>
            <th>Type</th>
            <th>User</th>
            <th>Session</th>
            <th class="details-col">Metadata</th>
        </tr>
        </thead>
        <tbody>
        <c:choose>
            <c:when test="${not empty events}">
                <c:forEach var="event" items="${events}">
                    <c:set var="detailUrl"
                           value="${pageContext.request.contextPath}/module/auditlogweb/viewSecurityAudit.form?eventId=${event.id}" />
                    <tr class="security-row" onclick="window.location.href='<c:out value="${detailUrl}"/>'">
                        <td><c:out value="${event.eventTime}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${fn:contains(event.eventType,'LOGIN_SUCCESS')}">
                                     <span class="badge badge-login-success"><c:out value="${event.eventType}"/></span>
                                </c:when>
                                <c:when test="${fn:contains(event.eventType, 'PASSWORD') || fn:contains(event.eventType, 'LOGOUT')}">
                                    <span class="badge badge-logout"><c:out value="${event.eventType}"/></span>
                                </c:when>
                                <c:when test="${fn:contains(event.eventType, 'LOGIN_FAILURE') ||  fn:contains(event.eventType, 'ACCOUNT')}">
                                    <span class="badge badge-security"><c:out value="${event.eventType}"/></span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge badge-password"><c:out value="${event.eventType}"/></span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty event.username}">
                                    <c:out value="${event.username}" />
                                </c:when>
                                <c:otherwise>system</c:otherwise>
                            </c:choose>
                        </td>
                        <td><c:out value="${event.sessionId}" default="-"/></td>
                        <td class="details-cell details-col">
                            <c:set var="ipText" value="${empty event.ipAddress ? '-' : event.ipAddress}"/>
                            <c:set var="userAgentText" value="${empty event.userAgent ? '-' : event.userAgent}"/>
                            <c:set var="detailsText" value="IP: ${ipText} | UA: ${userAgentText}"/>
                            <span class="details-preview">
                                <c:choose>
                                    <c:when test="${fn:length(detailsText) > 50}">
                                        <c:out value="${fn:substring(detailsText, 0, 50)}"/>...
                                    </c:when>
                                    <c:otherwise>
                                        <c:out value="${detailsText}"/>
                                    </c:otherwise>
                                </c:choose>
                            </span>
                            <button type="button" class="details-icon" aria-label="Details" title="Details">&#9432;</button>
                            <div class="details-tooltip"><c:out value="${detailsText}"/></div>
                        </td>
                    </tr>
                </c:forEach>
            </c:when>
            <c:otherwise>
                <tr>
                    <td colspan="5">No security audit logs found for the given criteria.</td>
                </tr>
            </c:otherwise>
        </c:choose>
        </tbody>
    </table>

    <div class="security-pagination">
        <div>Showing page <c:out value="${currentPage + 1}"/> of <c:out value="${totalPages > 0 ? totalPages : 1}"/> | Total records: <c:out value="${totalCount}"/></div>
        <div>
            <button type="button" class="pager-btn" onclick="goToPage(<c:out value='${currentPage - 1}'/>)" <c:if test="${!hasPreviousPage}">disabled</c:if>>Previous</button>
            <button type="button" class="pager-btn" onclick="goToPage(<c:out value='${currentPage + 1}'/>)" <c:if test="${!hasNextPage}">disabled</c:if>>Next</button>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>

<script>
    function goToPage(page) {
        if (page < 0) {
            return;
        }
        document.getElementById('pageInput').value = page;
        document.getElementById('securityAuditForm').submit();
    }
</script>
