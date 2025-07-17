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

<link rel="stylesheet" type="text/css" href="<openmrs:contextPath/>/moduleResources/auditlogweb/css/auditlogs.css" >

<div class="auditlog-container">
    <h2><spring:message code="Audit Logs" /></h2>
    <form action="auditlogs.form" method="post" id="auditForm" autocomplete="off">
        <div class="filter-group">
            <div class="filter-field">
                <label for="username" class="filter-label">Search by User:</label>
                <input type="text" id="username" name="username" placeholder="Enter username" value="${param.username}">
                <div id="usernameDropdown" class="dropdown-list"></div>
            </div>
            <div class="filter-field">
                <label for="startDate" class="filter-label">From:</label>
                <input type="date" id="startDate" name="startDate" value="${param.startDate}">
            </div>
            <div class="filter-field">
                <label for="endDate" class="filter-label">To:</label>
                <input type="date" id="endDate" name="endDate" value="${param.endDate}">
            </div>
        </div>

        <div class="search-group">
            <label for="entitySearch" class="search-label">Search and Select an Entity:</label>
            <input type="text" id="entitySearch" class="search-dropdown-input" placeholder="Type entity name to search..." readonly>
            <input type="hidden" name="selectedClass" id="selectedClass">

            <input type="hidden" name="page" id="pageInput" value="${currentPage != null ? currentPage : 0}" />
            <input type="hidden" name="size" id="hiddenPageSize" value="${pageSize != null ? pageSize : 15}" />

            <div id="dropdownList" class="dropdown-list"></div>
            <button type="submit" class="view-btn">View Audits</button>
        </div>
    </form>

    <c:if test="${not empty audits}">
        <h2>
            <c:choose>
                <c:when test="${not empty className}">Audit Table for ${className}</c:when>
                <c:otherwise>Audit Logs Table</c:otherwise>
            </c:choose>
        </h2>
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
            <c:forEach var="audit" items="${audits}">
                <c:set var="className" value="${audit.entity.getClass().getName()}" />
                <tr
                        <c:choose>
                            <c:when test="${not fn:contains('Role,GlobalProperty', audit.entityClassSimpleName)}">
                                onclick="window.location.href='${pageContext.request.contextPath}/module/auditlogweb/viewAudit.form?auditId=${audit.revisionEntity.id}&entityId=${audit.entity.id}&class=${className}'"
                            </c:when>
                            <c:otherwise>
                                onclick="window.location.href='${pageContext.request.contextPath}/module/auditlogweb/viewAudit.form?auditId=${audit.revisionEntity.id}&entityId=NA&class=${className}'"
                            </c:otherwise>
                        </c:choose>
                >
                    <td>
                        <c:choose>
                            <c:when test="${not fn:contains('Role,GlobalProperty', audit.entityClassSimpleName)}">
                                ${audit.entity.id}
                            </c:when>
                            <c:otherwise>
                                <i>N/A</i>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>${audit.changedBy}</td>
                    <td>${audit.revisionEntity.changedOn}</td>
                    <td>
                        <c:choose>
                            <c:when test="${audit.revisionType.name() == 'ADD'}"><spring:message code="auditlogweb.revisionType.add"/></c:when>
                            <c:when test="${audit.revisionType.name() == 'MOD'}"><spring:message code="auditlogweb.revisionType.mod"/></c:when>
                            <c:when test="${audit.revisionType.name() == 'DEL'}"><spring:message code="auditlogweb.revisionType.del"/></c:when>
                            <c:otherwise>${audit.revisionType.name()}</c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>

        <div class="pagination-controls">
            <div class="pagination-container">
                <c:forEach begin="0" end="${totalPages - 1}" var="i">
                    <button
                            type="button"
                            onclick="goToPage(${i})"
                            class="pagination-btn <c:if test='${i == currentPage}'>active-page</c:if>">
                            ${i + 1}
                    </button>
                </c:forEach>

                <!-- Page Size Selector -->
                <label for="size" class="page-size-label">Page Size:</label>
                <select name="size" id="size"
                        onchange="document.getElementById('pageInput').value = 0; document.getElementById('hiddenPageSize').value = this.value; document.getElementById('auditForm').submit();">
                    <option value="5"  <c:if test="${pageSize == 5}">selected</c:if>>5</option>
                    <option value="10" <c:if test="${pageSize == 10}">selected</c:if>>10</option>
                    <option value="15" <c:if test="${pageSize == 15}">selected</c:if>>15</option>
                    <option value="25" <c:if test="${pageSize == 25}">selected</c:if>>25</option>
                    <option value="50" <c:if test="${pageSize == 50}">selected</c:if>>50</option>
                </select>
            </div>
        </div>
    </c:if>

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
                        <tr
                                <c:choose>
                                    <c:when test="${not fn:contains('Role,GlobalProperty', audit.entityClassSimpleName)}">
                                        onclick="window.location.href='${pageContext.request.contextPath}/module/auditlogweb/viewAudit.form?auditId=${audit.revisionEntity.id}&entityId=${audit.entity.id}&class=${audit.entity.getClass().getName()}'"
                                    </c:when>
                                    <c:otherwise>
                                        onclick="window.location.href='${pageContext.request.contextPath}/module/auditlogweb/viewAudit.form?auditId=${audit.revisionEntity.id}&entityId=NA&class=${audit.entity.getClass().getName()}'"
                                    </c:otherwise>
                                </c:choose>
                        >
                            <td>
                                <c:choose>
                                    <c:when test="${not fn:contains('Role,GlobalProperty', audit.entityClassSimpleName)}">
                                        ${audit.entity.id}
                                    </c:when>
                                    <c:otherwise>
                                        <i>N/A</i>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>${audit.entityClassSimpleName}</td>
                            <td>${audit.changedBy}</td>
                            <td>${audit.changedOn}</td>
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