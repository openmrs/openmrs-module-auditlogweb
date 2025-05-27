<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ page import="java.lang.reflect.Field" %>
<%@include file="localHeader.jsp"%>

<%--<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/resources/css/viewAudit.css"/>--%>
<link rel="stylesheet" type="text/css" href="<openmrs:contextPath/>/moduleResources/auditlogweb/css/viewAudit.css" >
<div class="audit-container">
    <div class="audit-title">Audit Log Details</div>
    <table class="audit-table">
        <thead>
        <tr>
            <th>Field</th>
            <th>Old Value</th>
            <th>Current Value</th>
        </tr>
        </thead>
        <tbody id="auditTableBody">
        <%
            try {
                Object oldEntity = request.getAttribute("oldEntity");
                Object currentEntity = request.getAttribute("currentEntity");
                int rowIndex = 0;

                if (currentEntity != null) {
                    Class<?> currentEntityClass = currentEntity.getClass();

                    if (oldEntity != null) {
                        Class<?> oldEntityClass = oldEntity.getClass();

                        if (oldEntityClass.equals(currentEntityClass)) {
                            Field[] fields = currentEntityClass.getDeclaredFields();

                            for (Field field : fields) {
                                field.setAccessible(true);
                                Object oldValue = field.get(oldEntity);
                                Object currentValue = field.get(currentEntity);

                                boolean isDifferent = (oldValue != null && currentValue != null && !oldValue.toString().trim().equals(currentValue.toString().trim()))
                                        || (oldValue == null && currentValue != null);
                                String rowClass = (rowIndex % 2 == 0) ? "evenRow" : "oddRow";
                                out.println("<tr class='audit-row " + rowClass + (isDifferent ? " highlight" : "") + "'>");
                                out.println("<td>" + field.getName() + "</td>");
                                out.println("<td>" + (oldValue != null ? oldValue : "null") + "</td>");
                                out.println("<td>" + (currentValue != null ? currentValue : "null") + "</td>");
                                out.println("</tr>");
                                rowIndex++;
                            }
                        } else {
                            out.println("<tr class='audit-row'><td colspan='3'>The entities are of different types.</td></tr>");
                        }
                    } else {
                        Field[] fields = currentEntityClass.getDeclaredFields();
                        for (Field field : fields) {
                            field.setAccessible(true);
                            Object currentValue = null;
                            try {
                                currentValue = field.get(currentEntity);
                            } catch (IllegalAccessException e) {}

                            out.println("<tr class='audit-row'>");
                            out.println("<td>" + field.getName() + "</td>");
                            out.println("<td>null</td>");
                            out.println("<td>" + (currentValue != null ? currentValue : "null") + "</td>");
                            out.println("</tr>");
                            rowIndex++;
                        }
                    }
                } else {
                    out.println("<tr class='audit-row'><td colspan='3'>No entity data available.</td></tr>");
                }
            } catch (IllegalArgumentException ie) {
                if (ie.getCause() instanceof org.hibernate.QueryException) {
                    out.println("<tr class='audit-row'><td colspan='3' style='color: #b94a48; background: #f2dede;'>Some referenced data could not be loaded." +
                            " Please contact your administrator if this problem persists.</td></tr>");
                } else {
                    throw ie;
                }
            }
        %>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/template/footer.jsp"%>