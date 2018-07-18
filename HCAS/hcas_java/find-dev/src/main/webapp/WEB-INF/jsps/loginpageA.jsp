<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>

<!DOCTYPE html>
<html> 
	<head> 
    </head> 
    <body> 
	<form action="loginA.do" method="post"> 
	username:<br> 
	<input type="text" name="username" class="username span5" value=""><br> 
	password:<br> 
	<input type="text" name="password" class="password span5" value=""><br><br> 
	
	<input type="submit" value="Submit"> 
	</form> 
	</body> 
</html>
 