<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<% request.setAttribute("page", "auditlogs"); %>
<%@include file="localHeader.jsp"%>

<style>
    .auditlog-container {
        /*width: 90vw;*/
        max-width: none;
        margin-top: 8px;
        margin-bottom: 30px;
        background: #fff;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(0,0,0,0.08);
        padding: 32px 24px 24px 24px;
    }
    .search-group {
        position: relative;
        margin-bottom: 24px;
    }
    .search-label{
        display: block;
        font-size: 1em;
        margin-bottom: 8px;
        color: #444;
    }
    .search-dropdown-input {
        width: 100%;
        padding: 12px;
        border: 1px solid #bbb;
        border-radius: 4px;
        font-size: 1.1em;
        box-sizing: border-box;
    }
    .dropdown-list {
        position: absolute;
        z-index: 10;
        width: 100%;
        max-height: 220px;
        overflow-y: auto;
        background: #fff;
        border: 1px solid #bbb;
        border-top: none;
        border-radius: 0 0 4px 4px;
        box-shadow: 0 2px 8px rgba(0,0,0,0.08);
        display: none;
    }
    .view-btn {
        background: #0a9a7e;
        color: #fff;
        border: none;
        padding: 10px 24px;
        border-radius: 4px;
        font-size: 1.1em;
        cursor: pointer;
        margin-top: 8px;
        display: block;
    }
    .audit-table {
        width: 100%;
        border-collapse: collapse;
        margin-top: 20px;
    }
    .audit-table th, .audit-table td {
        padding: 10px 8px;
        border-bottom: 1px solid #eee;
        text-align: left;
    }
    .audit-table tr {
        cursor: pointer;
        transition: background 0.15s;
    }
    .audit-table tr:hover {
        background: #f5f7fa;
    }
    .filter-field input[type="text"],
    .filter-field input[type="date"] {
        max-width: 160px;
        width: 100%;
        padding: 12px;
        font-size: 0.98em;
        box-sizing: border-box;
    }
    .filter-group {
        display: flex;
        flex-direction: row;
        gap: 16px;
        flex-wrap: wrap;
        margin-bottom: 18px;
        align-items: flex-end;
    }
    .filter-field {
        display: flex;
        flex-direction: column;
        flex: none;
    }
    .filter-label {
        font-size: 1em;
        margin-bottom: 4px;
        color: #444;
    }
    @media (max-width: 700px) {
        .filter-group { flex-direction: column; gap: 8px; }
    }
</style>
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

    const input = document.getElementById('entitySearch');
    const hiddenInput = document.getElementById('selectedClass');
    const dropdown = document.getElementById('dropdownList');

    function renderDropdown(filter = "") {
        dropdown.innerHTML = "";
        let found = false;
        classes.forEach(cls => {
            if (cls.toLowerCase().includes(filter.toLowerCase())) {
                const div = document.createElement('div');
                div.className = 'dropdown-item' + (cls === currentClass ? ' selected' : '');
                div.textContent = cls;
                div.onclick = function() {
                    input.value = cls;
                    hiddenInput.value = cls;
                    dropdown.style.display = "none";
                    dropdown.classList.remove('active');
                    document.getElementById('auditForm').submit();
                };
                dropdown.appendChild(div);
                found = true;
            }
        });
        dropdown.style.display = found ? "block" : "none";
        dropdown.classList.toggle('active', found);
    }

    input.addEventListener('focus', () => renderDropdown(input.value));
    input.addEventListener('click', () => renderDropdown(input.value));
    input.removeAttribute('readonly');
    input.addEventListener('input', () => renderDropdown(input.value));

    document.addEventListener('click', function(e) {
        if (!input.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.style.display = "none";
            dropdown.classList.remove('active');
        }
    });

    if (currentClass) {
        input.value = currentClass;
        hiddenInput.value = currentClass;
    }
</script>