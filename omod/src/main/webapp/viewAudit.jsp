<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ page import="java.lang.reflect.Field" %>
<%@ page import="org.hibernate.QueryException" %>

<style>
    .audit-container {
        max-width: 900px;
        margin: 40px auto;
        background: #fff;
        border-radius: 10px;
        box-shadow: 0 4px 24px rgba(0,0,0,0.08);
        padding: 32px 24px 24px 24px;
    }
    .audit-title {
        font-size: 2em;
        font-weight: 600;
        margin-bottom: 24px;
        color: #2c3e50;
        letter-spacing: 0.5px;
    }
    table.audit-table {
        width: 100%;
        border-collapse: separate;
        border-spacing: 0;
        background: #fafbfc;
        border-radius: 8px;
        overflow: hidden;
        box-shadow: 0 1px 4px rgba(0,0,0,0.03);
    }
    .audit-table th, .audit-table td {
        padding: 14px 12px;
        text-align: left;
    }
    .audit-table th {
        background: #f4f6fa;
        color: #34495e;
        font-size: 1.05em;
        font-weight: 600;
        border-bottom: 2px solid #e1e4ea;
    }
    .audit-table tr {
        transition: background 0.15s;
    }
    .audit-table tr:nth-child(even) {
        background: #f9fafb;
    }
    .audit-table tr:hover {
        background: #eaf6ff;
    }
    .audit-table .highlight {
        background: #fffbe6 !important;
        font-weight: 500;
        color: #b26a00;
    }
    .audit-table td {
        border-bottom: 1px solid #e1e4ea;
        font-size: 1em;
    }
    .audit-table td:first-child {
        font-family: 'Fira Mono', 'Consolas', monospace;
        color: #2d6a4f;
    }
    @media (max-width: 700px) {
        .audit-container { padding: 12px; }
        .audit-title { font-size: 1.2em; }
        .audit-table th, .audit-table td { padding: 8px 4px; font-size: 0.95em; }
    }
</style>
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
        <tbody>
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
                                out.println("<tr class='" + rowClass + (isDifferent ? " highlight" : "") + "'>");
                                out.println("<td>" + field.getName() + "</td>");
                                out.println("<td>" + (oldValue != null ? oldValue : "null") + "</td>");
                                out.println("<td>" + (currentValue != null ? currentValue : "null") + "</td>");
                                out.println("</tr>");
                                rowIndex++;
                            }
                        } else {
                            out.println("<tr><td colspan='3'>The entities are of different types.</td></tr>");
                        }
                    } else {
                        Field[] fields = currentEntityClass.getDeclaredFields();
                        for (Field field : fields) {
                            field.setAccessible(true);
                            Object currentValue = null;
                            try {
                                currentValue = field.get(currentEntity);
                            } catch (IllegalAccessException e) {}

                            out.println("<tr>");
                            out.println("<td>" + field.getName() + "</td>");
                            out.println("<td>null</td>");
                            out.println("<td>" + (currentValue != null ? currentValue : "null") + "</td>");
                            out.println("</tr>");
                            rowIndex++;
                        }
                    }
                } else {
                    out.println("<tr><td colspan='3'>No entity data available.</td></tr>");
                }
            } catch (IllegalArgumentException ie) {
                if (ie.getCause() instanceof org.hibernate.QueryException) {
                    out.println("<tr><td colspan='3' style='color: #b94a48; background: #f2dede;'>Some referenced data could not be loaded. Please contact your administrator if this problem persists.</td></tr>");
                } else {
                    throw ie;
                }
            }
        %>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/template/footer.jsp"%>