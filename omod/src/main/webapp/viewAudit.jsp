<%@ include file="/WEB-INF/template/include.jsp" %>
<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="localHeader.jsp" %>

<link rel="stylesheet" type="text/css" href="<openmrs:contextPath/>/moduleResources/auditlogweb/css/viewAudit.css" />

<div class="audit-container">
    <div class="audit-title">Audit Log Details</div>

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
