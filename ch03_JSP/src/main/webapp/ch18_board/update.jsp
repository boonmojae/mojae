<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="kr.board.dao.BoardDAO"%>
<%@ page import="kr.board.vo.BoardVO"%>
<%
	//전송된 데이터 인코딩 타입 지정
	request.setCharacterEncoding("utf-8");
%>
<jsp:useBean id="boardVO" class="kr.board.vo.BoardVO"/>
<jsp:setProperty property="*" name="boardVO"/><!-- 자바빈에 접근하기 위해서 id,name 이름 동일해야됨 -->
<%
	BoardDAO dao = BoardDAO.getInstance();
	//비밀번호 인증을 위해서 기본키를 전달하고 한 건의 레코드를 전달받음
	BoardVO db_board = dao.getBoard(boardVO.getNum());
	boolean check = false;
	if(db_board!=null){
		//비밀번호 체크
		check = db_board.isCheckedPassword(boardVO.getPasswd());
	}
	if(check){//인증 성공
		boardVO.setIp(request.getRemoteAddr());
		dao.update(boardVO);
%>
	<script>
		alert('글 수정을 완료했습니다');
		location.href='detail.jsp?num=<%= boardVO.getNum() %>';
	</script>
<%
	}else{//인증 실패
%>
	<script>
		alert('비밀번호 불일치');
		history.go(-1);
	</script>
<%		
	}
%>