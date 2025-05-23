<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<style>
    .existing-resources th,td {
        padding-right: 1em;
    }
</style>

<h2><spring:message code="auditlogweb.title" /> </h2>

<br/>
<h2>Select a Class for Audit</h2>

<form action="allAudits.form" method="post">
    <select name="selectedClass">
        <c:forEach var="cls" items="${classes}">
            <option value="${cls}" <c:if test="${cls == currentClass}">selected</c:if>>${cls}</option>
        </c:forEach>
    </select>
    <button type="submit">View Audits</button>
</form>

<p>${test}</p>
<c:if test="${not empty audits}">
    <h2>Audit Table for ${currentClass}</h2>
    <div class="box">
        <table class="existing-resources">
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
                <tr valign="top" class="${status.index % 2 == 0 ? "evenRow" : "oddRow"}"
                    onclick="window.location.href='${pageContext.request.contextPath}/module/auditlogweb/viewAudit.form?auditId=${audit.revisionEntity.id}&entityId=${audit.entity.id}&class=${currentClass}'">
                    <td>${audit.entity.id}</td>
                    <td>${audit.changedBy}</td>
                    <td>${audit.revisionEntity.changedOn}</td>
                    <td>${audit.revisionType.name()}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>


</c:if>
<%@ include file="/WEB-INF/template/footer.jsp"%>
