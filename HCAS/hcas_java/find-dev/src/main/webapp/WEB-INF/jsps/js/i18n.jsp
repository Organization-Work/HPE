<%@page contentType="text/javascript" %><%--
--%><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %><%--
--%><%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %><%--
--%><%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %><%--
--%><spring:htmlEscape defaultHtmlEscape="false"/>
define(['messageformat'], function(MessageFormat){
    var mf = new MessageFormat('en');
    var i18n = {
        <%-- listing properties here since we don't want to have to return all of them --%>
        <c:forTokens items="
            idolview.svg.browser.required,
            idolview.viewer.resultcount,
            idolview.filter.leaf.options.html,
            idolview.filter.droptarget,
            idolview.center.label.numdocs,
            idolview.Parameter,
            idolview.Query,
            graph.daterange.label,
            graph.point.label,
            graph.filter.set,
            graph.filter.none,
            graph.filter.clear
            " delims="," var="key" varStatus="status"><%--
        --%><c:set var="key" value="${fn:trim(key)}"/><%--
        --%>'<spring:escapeBody javaScriptEscape="true">${key}</spring:escapeBody>': '<spring:message code="${key}" javaScriptEscape="true"/>'<%--
        --%>${!status.last ? ',' : ''}
        </c:forTokens>
    };

    for (var key in i18n) {
        if (i18n.hasOwnProperty(key)) {
            i18n[key] = mf.compile(i18n[key]);
        }
    }

    return function(key, props) {
        var fn = i18n[key];
        return !fn ? key : fn instanceof Function ? fn(props) : fn;
    }
});
