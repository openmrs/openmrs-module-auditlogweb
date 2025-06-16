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
<%@include file="localHeader.jsp"%>

<link rel="stylesheet" type="text/css" href="<openmrs:contextPath/>/moduleResources/auditlogweb/css/auditlogs.css" >

<div class="auditlog-container">
    <h2><spring:message code="Audit Logs" /></h2>
    <form action="auditlogs.form" method="post" id="auditForm" autocomplete="off">
        <div class="filter-group">
            <div class="filter-field">
                <label for="username" class="filter-label">Search by User: </label>
                <input type="text" id="username" name="username" placeholder="Enter username" value="${param.username}">
            </div>
            <div class="filter-field">
                <label for="startDate" class="filter-label">From: </label>
                <input type="date" id="startDate" name="startDate" value="${param.startDate}">
            </div>
            <div class="filter-field">
                <label for="endDate" class="filter-label">To: </label>
                <input type="date" id="endDate" name="endDate" value="${param.endDate}">
            </div>
        </div>
        <div class="search-group">
            <label for="entitySearch" class="search-label">Search and Select an Entity:</label>
            <input type="text" id="entitySearch" class="search-dropdown-input" placeholder="Type entity name to search..." readonly>
            <input type="hidden" name="selectedClass" id="selectedClass" required>
            <div id="dropdownList" class="dropdown-list"></div>
            <button type="submit" class="view-btn" >View Audits</button>
        </div>
    </form>

    <c:if test="${not empty audits}">
        <h2>Audit Table for ${currentClassi}</h2>
        <table class="audit-table">
            <thead>
            <tr>
                <th>ID</th>
                <th>Changed By</th>
                <th>Changed On</th>
                <th>Revision Type</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="audit" items="${audits}" varStatus="status">
                <tr onclick="window.location.href='${pageContext.request.contextPath}/module/auditlogweb/viewAudit.form?auditId=${audit.revisionEntity.id}&entityId=${audit.entity.id}&class=${currentClass}'">
                    <td>${audit.entity.id}</td>
                    <td>${audit.changedBy}</td>
                    <td>${audit.revisionEntity.changedOn}</td>
                    <td>${audit.revisionType.name()}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>
    <br>
    <section class="recent-audits-section">
        <h2>Recent Audit Trails</h2>
        <c:choose>
            <c:when test="${not empty recentAudits}">
                <table class="audit-table">
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Entity Class</th>
                        <th>Changed By</th>
                        <th>Changed On</th>
                        <th>Revision Type</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="audit" items="${recentAudits}">
                        <tr onclick="window.location.href='${pageContext.request.contextPath}/module/auditlogweb/viewAudit.form?auditId=${audit.revisionEntity.id}&entityId=${audit.entity.id}&class=${audit.entity.getClass().getName()}'">
                            <td>${audit.entity.id}</td>
                            <td>${audit.entity.getClass().getSimpleName()}</td>
                            <td>${audit.changedBy}</td>
                            <td>${audit.revisionEntity.changedOn}</td>
                            <td>${audit.revisionType.name()}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:when>
            <c:otherwise>
                <div style="padding: 24px; color: #888;">No recent audit events to display.</div>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>
<script>
    const classes = [
        <c:forEach var="cls" items="${classes}" varStatus="status">
        "${cls}"<c:if test="${!status.last}">,</c:if>
        </c:forEach>
    ];
    const currentClass = "${currentClass}";
</script>
<script src="<openmrs:contextPath/>/moduleResources/auditlogweb/scripts/auditlogs.js"></script>
