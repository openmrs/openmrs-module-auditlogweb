<ul id="menu">
    <li class="first">
        <a href="${pageContext.request.contextPath}/admin/index.htm"><openmrs:message code="admin.title.short"/></a>
    </li>
    <li <c:if test='${page eq "auditlogs"}'>class="active"</c:if>>
        <a href="${pageContext.request.contextPath}/module/auditlogweb/auditlogs.form">Audit Logs</a>
    </li>
    <li <c:if test='${page eq "viewAudit"}'>class="active"</c:if>>
        <a href="${pageContext.request.contextPath}/module/auditlogweb/viewAudit.form">Audit Log Detail</a>
    </li>
    <!-- Add more tabs as needed -->
</ul>