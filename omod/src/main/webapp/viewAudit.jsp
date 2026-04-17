<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%--
  This Source Code Form is subject to the terms of the Mozilla Public License,
  v. 2.0. If a copy of the MPL was not distributed with this file, You can
  obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
  the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
  Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
  graphic logo is a trademark of OpenMRS Inc.
--%>
<%@ include file="/WEB-INF/template/include.jsp" %>
<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="localHeader.jsp" %>

<link rel="stylesheet" type="text/css" href="<openmrs:contextPath/>/moduleResources/auditlogweb/css/viewAudit.css" />

<div class="audit-container">
    <div class="audit-title">Audit Log Details</div>

    <div class="audit-meta-data">
        <div class="audit-meta-item"><strong>Audit Type:</strong> <c:out value="${auditType}" /></div>
        <div class="audit-meta-item"><strong>Entity Type:</strong> <c:out value="${entityType}" /></div>
        <div class="audit-meta-item"><strong>Changed By:</strong> <c:out value="${changedBy}" /></div>
        <div class="audit-meta-item"><strong>Changed On:</strong> <fmt:formatDate value="${changedOn}" pattern="yyyy-MM-dd HH:mm:ss" /></div>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="audit-error">
                ${errorMessage}
        </div>
    </c:if>

    <c:if test="${not empty relatedEntities}">
        <div class="related-entities-section">
            <div class="toggle-header" onclick="toggleRelatedEntities()">
                <span class="toggle-icon">&#9658;</span>
                Related Entities Changed in This Revision (${relatedEntities.size()})
            </div>
            <div class="related-entities-content" id="relatedEntitiesContent">
                <table class="related-entities-table">
                    <thead>
                        <tr>
                            <th>Entity Type</th>
                            <th>Entity ID</th>
                            <th>Change Type</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="related" items="${relatedEntities}">
                            <tr>
                                <td><c:out value="${related.simpleName}" /></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${related.entityIdValue != 'unknown' && related.entityIdValue != ''}">
                                            <c:out value="${related.entityIdValue}" />
                                        </c:when>
                                        <c:otherwise>
                                            N/A
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${related.revisionType.name() == 'ADD'}">Creation</c:when>
                                        <c:when test="${related.revisionType.name() == 'MOD'}">Modification</c:when>
                                        <c:when test="${related.revisionType.name() == 'DEL'}">Deletion</c:when>
                                        <c:otherwise><c:out value="${related.revisionType.name()}" /></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:if test="${related.entityIdValue != 'unknown' && related.entityIdValue != ''}">
                                        <c:url var="viewAuditUrl" value="/module/auditlogweb/viewAudit.form">
                                            <c:param name="auditId" value="${related.revisionId}" />
                                            <c:param name="entityId" value="${related.entityIdValue}" />
                                            <c:param name="class" value="${related.className}" />
                                        </c:url>
                                        <a href="${viewAuditUrl}">View Audit</a>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </c:if>

    <table class="audit-table">
        <thead>
        <tr>
            <th>Field</th>
            <th>Old Value</th>
            <th>Current Value</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="diff" items="${diffs}" varStatus="status">
            <tr class="audit-row ${status.index % 2 == 0 ? 'evenRow' : 'oddRow'} ${diff.changed ? 'highlight' : ''}">
                <td><c:out value="${diff.fieldName}" /></td>
                <td><c:out value="${diff.oldValue}" /></td>
                <td><c:out value="${diff.currentValue}" /></td>
            </tr>
        </c:forEach>

        <c:if test="${empty diffs}">
            <tr><td colspan="3">No entity data available.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>

<script>
    function toggleRelatedEntities() {
        var content = document.getElementById('relatedEntitiesContent');
        var icon = document.querySelector('.toggle-icon');
        if (content.style.display === 'none' || content.style.display === '') {
            content.style.display = 'block';
            icon.innerHTML = '&#9660;';
        } else {
            content.style.display = 'none';
            icon.innerHTML = '&#9658;';
        }
    }
</script>

<%@ include file="/WEB-INF/template/footer.jsp" %>
