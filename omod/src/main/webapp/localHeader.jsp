<%--
  This Source Code Form is subject to the terms of the Mozilla Public License,
  v. 2.0. If a copy of the MPL was not distributed with this file, You can
  obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
  the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
  Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
  graphic logo is a trademark of OpenMRS Inc.
--%>
<ul id="menu">
    <li class="first">
        <a href="${pageContext.request.contextPath}/admin/index.htm"><openmrs:message code="admin.title.short"/></a>
    </li>
    <li <c:if test='${page eq "auditlogs"}'>class="active"</c:if>>
        <a href="${pageContext.request.contextPath}/module/auditlogweb/auditlogs.form">Audit Logs</a>
    </li>
    <!-- Add more tabs as needed -->
</ul>