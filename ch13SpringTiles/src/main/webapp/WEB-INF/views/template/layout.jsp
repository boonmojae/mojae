<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title><tiles:getAsString name="title"/></title><!-- 설정파일에 있는 title읽어옴 -->
</head>
<body>
<table style="width:100%;">
	<tr height="100" valign="middle" bgcolor="FFFFFF">
		<td colspan="2"><tiles:insertAttribute name="header"/></td>
	</tr>
	<tr height="500">
		<td width="15%" valign="top">
			<tiles:insertAttribute name="menu"/>
		</td>
		<td width="85%" align="center">
			<tiles:insertAttribute name="body"/>
		</td>
	</tr>
	<tr>
		<td colspan="2"><tiles:insertAttribute name="footer"/></td>
	</tr>
</table>
</body>
</html>