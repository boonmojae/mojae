package kr.spring.board.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import kr.spring.board.service.BoardService;
import kr.spring.board.vo.BoardVO;
import kr.spring.member.vo.MemberVO;
import kr.spring.util.FileUtil;
import kr.spring.util.PagingUtil;
import kr.spring.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;


@Slf4j
@Controller
public class BoardController {
	@Autowired
	private BoardService boardService;
	/*===============================
	 *  게시판 글쓰기
	 ===============================*/
	// 자바빈 초기화
	@ModelAttribute
	public BoardVO initCommand() {
		return new BoardVO();
	}

	@GetMapping("/board/write")
	public String formWrite() {
		return "boardWrite";
	}

	@PostMapping("/board/write")
	public String submit(@Valid BoardVO boardVO, 
			BindingResult result,  
			HttpServletRequest request, 
			HttpSession session,
			Model model) throws IllegalStateException, IOException {
		
		log.debug("<<게시판 글쓰기>> : " + boardVO);
		
		if(result.hasErrors()) {
			return formWrite();
		}
		
		MemberVO user = (MemberVO)session.getAttribute("user");
		boardVO.setMem_num(user.getMem_num());
		boardVO.setIp(request.getRemoteAddr());
		
		// 파일 업로드 처리
		boardVO.setFilename(FileUtil.createFile(request, boardVO.getUpload()));


		boardService.insertBoard(boardVO);
		
		
		model.addAttribute("message", "성공적으로 글이 등록되었습니다");
		model.addAttribute("url",  request.getContextPath() + "/board/list");
		return "common/resultAlert";
	}


	/*===============================
	 *  게시판 목록
	 ===============================*/
	@GetMapping("/board/list")
	public String getList( @RequestParam(defaultValue="1") int pageNum, 
											 @RequestParam(defaultValue = "1") int order, 
											 @RequestParam(defaultValue = "") String category,
											 String keyfield, String keyword, Model model) {
		log.debug("<<게시판 목록 - category>> : " + category);
		log.debug("<<게시판 목록 - order>> : " + order);
		
		Map<String,Object> map = new HashMap<String,Object> ();
		map.put("category", category);
		map.put("keyfield", keyfield);
		map.put("keyword", keyword);
		
		//전체,검색 레코드수
		int count = boardService.selectRowCount(map);
		log.debug("<<게시판 목록 - count>> : " + count);
		//페이지 처리
		PagingUtil page = new PagingUtil(keyfield,keyword,pageNum,count,20,10,"list","&category="+category+"&order="+order);//&category부가적으로 보내지는 파라미터
		//목록
		List<BoardVO> list = null;
		if(count > 0) {
			map.put("order", order);
			map.put("start", page.getStartRow());
			map.put("end", page.getEndRow());
			
			list = boardService.selectList(map);
		}
		model.addAttribute("count",count);
		model.addAttribute("list",list);
		model.addAttribute("page",page.getPage());
		
		return "boardList";
	}
	/*===============================
	 *  게시판 글상세
	 ===============================*/
	@GetMapping("/board/detail")
	public ModelAndView process(long board_num) {
		log.debug("<<게시판 글 상세 - board_num>> : " + board_num);
		
		//해당 글의 조회수 증가
		boardService.updateHit(board_num);
		BoardVO board = boardService.selectBoard(board_num);
		
		//제목에 태그를 허용하지 않음
		board.setTitle(StringUtil.useNoHTML(board.getTitle()));
		
		//내용에 태그를 허용하지 않으면서 줄바꿈 처리 (CKEditor 사용시 주석 처리)
		//board.setContent(StringUtil.useBrNoHTML(board.getContent()));
		
		return new ModelAndView("boardView","board",board);
	}
	//파일 다운로드
	@GetMapping("/board/file")
	public String download(long board_num,HttpServletRequest request,Model model) {
		BoardVO board = boardService.selectBoard(board_num);//byte배열로 처리해야 다운 가능
		byte[] downloadFile = FileUtil.getBytes(request.getServletContext().getRealPath("/upload")+"/"+board.getFilename());
		
		model.addAttribute("downloadFile",downloadFile);
		model.addAttribute("filename",board.getFilename());
		
		return "downloadView";
	}
	/*===============================
	 *  게시판 글수정
	 ===============================*/
	//수정 폼 호출
	@GetMapping("/board/update")
	public String formUpdate(long board_num,Model model) {
		BoardVO boardVO = boardService.selectBoard(board_num);
		model.addAttribute("boardVO",boardVO);
		
		return "boardModify";
	}
	//수정 폼에서 전송된 데이터 처리
	@PostMapping("/board/update")
	public String submitUpdate(@Valid BoardVO boardVO, BindingResult result,Model model,HttpServletRequest request) throws IllegalStateException, IOException {// BoardVO,BindingResult 사이에 다른게 들어가면 에러남
		log.debug("<<게시판 글 수정 >> : " + boardVO);
		
		//유효성 체크 결과 오류가 있으면 폼 호출
		if(result.hasErrors()) {
			//BoardVO에 파일명 없음/유효성 걸려서 form호출하면 파일정보가 없어서 다시 셋팅해야됨
			//title 또는 content가 입력되지 않아서 유효성 체크에 걸리면 파일 정보를 잃어버리기 때문에 폼을 호출할 때 다시 파일 정보를 셋팅해야 함
			BoardVO vo = boardService.selectBoard(boardVO.getBoard_num());
			boardVO.setFilename(vo.getFilename());
			return "boardModify";
		}
		
		//DB에 저장된 파일 정보 구하기
		BoardVO db_board = boardService.selectBoard(boardVO.getBoard_num());
		//파일명 셋팅(FileUtil.createFile에서 파일이 없으면 null 처리함)
		boardVO.setFilename(FileUtil.createFile(request, boardVO.getUpload()));
		
		//ip셋팅
		boardVO.setIp(request.getRemoteAddr());
		
		//글 수정
		boardService.updateBoard(boardVO);
		
	if(boardVO.getUpload() != null && !boardVO.getUpload().isEmpty()) {
		//수정전 파일 삭제 처리
		FileUtil.removeFile(request, db_board.getFilename());
	}
	
		//View에 표시할 메시지
		model.addAttribute("message", "글 수정 완료");
		model.addAttribute("url", request.getContextPath() + "/board/detail?board_num="+boardVO.getBoard_num());

		//자바스크립트로 처리
		return "common/resultAlert";
	}
	/*===============================
	 *  게시판 글 삭제
	 ===============================*/
	@GetMapping("/board/delete")
	public String submitdelete(long board_num,HttpServletRequest request) {
		
		log.debug("<<게시판 글 삭제 - board_num >> : " + board_num); 
		
		//DB에 저장된 파일 정보 구하기
		BoardVO db_board = boardService.selectBoard(board_num);
		
		//글 삭제
		boardService.deleteBoard(board_num);
		
		if(db_board.getFilename()!=null) {
			//파일 삭제
			FileUtil.removeFile(request, db_board.getFilename());
		}
		
		return "redirect:/board/list";
	}
	
	
}
