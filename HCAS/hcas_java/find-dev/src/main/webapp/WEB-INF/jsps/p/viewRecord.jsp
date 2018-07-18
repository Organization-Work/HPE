<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib uri="sitemesh-decorator" prefix="decorator"%>
<%@ taglib uri="sitemesh-page" prefix="page"%>
<%@ taglib prefix="json" uri="/WEB-INF/tld/json.tld" %>
<html>
<body>
<input type="hidden" id="buildVersionConfig" value="-${config.version}">
    <div>
        <h2>Admission Info</h2>
        <div>Admission ID: ${fn:escapeXml(doc.documentFields['HADM_ID'][0].value)}</div>
        <div>Admission Date: ${fn:escapeXml(doc.documentFields['ADMISSION_DATE'][0].value)}</div>
        <div>Discharge Date: ${fn:escapeXml(doc.documentFields['DISCHARGE_DATE'][0].value)}</div>
    </div>
    <div>
        <h2>Patient Info</h2>
            <div>Patient ID: ${fn:escapeXml(doc.documentFields['PATIENT/SUBJECT_ID'][0].value)}</div>
            <div>Gender: ${fn:escapeXml(doc.documentFields['PATIENT/SEX'][0].value)} </div>
            <div>Date of Birth: ${fn:escapeXml(doc.documentFields['PATIENT/DOB'][0].value)} </div>
            <div>Date of Death: ${fn:escapeXml(doc.documentFields['PATIENT/DOD'][0].value)} </div>
            <div>Age at Admission: ${fn:escapeXml(doc.documentFields['PATIENT/AGE_AT_ADMISSION'][0].value)} </div>
            <div>Expired at Hospital: ${fn:escapeXml(doc.documentFields['PATIENT/HOSPITAL_EXPIRE'][0].value)} </div>
            <div>Religion: ${fn:escapeXml(doc.documentFields['PATIENT/RELIGION'][0].value)} </div>
            <div>Admission Source: ${fn:escapeXml(doc.documentFields['PATIENT/ADMISSION_SOURCE'][0].value)} </div>
            <div>Admission Type: ${fn:escapeXml(doc.documentFields['PATIENT/ADMISSION_TYPE'][0].value)} </div>
            <div>Marital Status: ${fn:escapeXml(doc.documentFields['PATIENT/MARITAL_STATUS'][0].value)} </div>
            <div>Overall Payor Group: ${fn:escapeXml(doc.documentFields['PATIENT/OVERALL_PAYOR_GROUP'][0].value)} </div>
            <div>Ethnicity: ${fn:escapeXml(doc.documentFields['PATIENT/ETHNICITY'][0].value)} </div>
    </div>
    <div>
        <h2>Diagnosis Codes</h2>
        <c:forEach var="item" items="${doc.documentFields.ICD9}">
            <div>${fn:escapeXml(item.value)}</div>
        </c:forEach>
    </div>
    <div>
        <h2>Procedure Codes</h2>
        <c:forEach var="item" varStatus="stat" items="${doc.documentFields['DRG/CODE']}">
            <div>Code: ${fn:escapeXml(item.value)}</div>
            <div>ID: ${fn:escapeXml(doc.documentFields['DRG/ITEMID'][stat.index].value)}</div>
            <div>Cost Weight: ${fn:escapeXml(doc.documentFields['DRG/COST_WEIGHT'][stat.index].value)}</div>
        </c:forEach>
    </div>
    <script>
  
    </script>
    </body>
</html>