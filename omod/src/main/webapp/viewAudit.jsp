<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ page import="java.lang.reflect.Field" %>

<style>
    .highlight {
        background-color: yellow;
    }
</style>

<div class="box">
    <table class="existing-resources">
        <thead>
        <tr>
            <th>Field</th>
            <th>Old Value</th>
            <th>Current Value</th>
        </tr>
        </thead>
        <tbody>
        <%
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
                            try {
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
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        out.println("<tr><td colspan='3'>The entities are of different types.</td></tr>");
                    }
                } else {
                    Field[] fields = currentEntityClass.getDeclaredFields();
                    for (Field field : fields) {
                        field.setAccessible(true);
                        try {
                            Object currentValue = field.get(currentEntity);

                            out.println("<tr>");
                            out.println("<td>" + field.getName() + "</td>");
                            out.println("<td>null</td>");
                            out.println("<td>" + (currentValue != null ? currentValue : "null") + "</td>");
                            out.println("</tr>");
                            rowIndex++;
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                out.println("<tr><td colspan='3'>No entity data available.</td></tr>");
            }
        %>
        </tbody>
    </table>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>