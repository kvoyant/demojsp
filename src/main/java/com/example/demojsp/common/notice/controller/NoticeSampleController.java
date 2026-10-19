package com.example.demojsp.common.notice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * [공통#28 샘플] 공지사항 화면.
 * <ul>
 *   <li>/common/notice/sample : APP 메인(홈) - 띠 공지 배치 + 메인 팝업 (이어하기 카드와 분리)</li>
 *   <li>/common/notice/list   : 공지 목록 - 전체 / 상품별 / 업무별</li>
 *   <li>/common/notice/detail : 공지 상세 (?id=) - 띠를 누르면 이동하는 화면</li>
 *   <li>/common/notice/admin  : 디지털어드민 공지사항 관리</li>
 * </ul>
 */
@Controller
public class NoticeSampleController {

    @GetMapping("/common/notice/sample")
    public String sample() {
        return "common/notice/sample";
    }

    @GetMapping("/common/notice/list")
    public String list() {
        return "common/notice/list";
    }

    @GetMapping("/common/notice/detail")
    public String detail() {
        return "common/notice/detail";
    }

    @GetMapping("/common/notice/admin")
    public String admin() {
        return "common/notice/admin";
    }
}
