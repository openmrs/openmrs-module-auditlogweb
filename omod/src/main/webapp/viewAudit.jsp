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

    <div class="audit-type">
        Audit Type: <c:out value="${auditType}" />
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="audit-error">
                ${errorMessage}
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

<%@ include file="/WEB-INF/template/footer.jsp" %>
