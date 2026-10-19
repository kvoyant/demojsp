package com.example.demojsp.common.producttab.controller;

import com.example.demojsp.common.producttab.dto.ProductTabView;
import com.example.demojsp.common.producttab.dto.PtItemDto;
import com.example.demojsp.common.producttab.dto.PtProductDto;
import com.example.demojsp.common.producttab.dto.PtSectionDto;
import com.example.demojsp.common.producttab.dto.PtSlideDto;
import com.example.demojsp.common.producttab.service.ProductTabAdminService;
import com.example.demojsp.common.producttab.service.ProductTabService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * [디지털어드민] 상품 탭 관리 API (공통#26 세부항목 2)
 *
 * <pre>
 *  GET  /api/common/product-tab-admin/slides                     슬라이드 카드 전체 (게시종료·미사용 포함)
 *  POST /api/common/product-tab-admin/slides                     슬라이드 등록(slideId 없음) / 수정(slideId 있음)
 *  GET  /api/common/product-tab-admin/sections                   섹션 목록 (연결 상품 수)
 *  POST /api/common/product-tab-admin/sections                   섹션 등록/수정 (사용여부 N = 화면에서 삭제)
 *  GET  /api/common/product-tab-admin/sections/{cd}/items        섹션의 상품 목록 (판매중지 포함)
 *  PUT  /api/common/product-tab-admin/sections/{cd}/items        섹션의 상품 목록 통째 저장
 *  GET  /api/common/product-tab-admin/products                   상품 원장 (선택 상자)
 *  GET  /api/common/product-tab-admin/preview?channel=&baseDt=   기준일 사용자 화면 미리보기
 * </pre>
 */
@RestController
@RequestMapping("/api/common/product-tab-admin")
public class ProductTabAdminApiController {

    private final ProductTabAdminService adminService;
    private final ProductTabService service;
    private final ProductTabAdminAuth auth;

    public ProductTabAdminApiController(ProductTabAdminService adminService, ProductTabService service, ProductTabAdminAuth auth) {
        this.adminService = adminService;
        this.service = service;
        this.auth = auth;
    }

    // ---------------------------------------------------------------- 슬라이드
    @GetMapping("/slides")
    public ApiResponse<List<PtSlideDto>> slides(HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(adminService.slides());
    }

    @PostMapping("/slides")
    public ApiResponse<PtSlideDto> saveSlide(@RequestBody PtSlideDto body, HttpServletRequest request) {
        boolean create = body.getSlideId() == null;
        PtSlideDto saved = adminService.saveSlide(body, auth.requireAdmin(request));
        return ApiResponse.success(create ? "슬라이드 카드가 등록되었습니다." : "슬라이드 카드가 수정되었습니다.", saved);
    }

    // ---------------------------------------------------------------- 섹션 · 상품
    @GetMapping("/sections")
    public ApiResponse<List<PtSectionDto>> sections(HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(adminService.sections());
    }

    @PostMapping("/sections")
    public ApiResponse<PtSectionDto> saveSection(@RequestBody PtSectionDto body, HttpServletRequest request) {
        return ApiResponse.success("섹션이 저장되었습니다.", adminService.saveSection(body, auth.requireAdmin(request)));
    }

    @GetMapping("/sections/{sectionCd}/items")
    public ApiResponse<List<PtItemDto>> items(@PathVariable("sectionCd") String sectionCd, HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(adminService.sectionItems(sectionCd));
    }

    @PutMapping("/sections/{sectionCd}/items")
    public ApiResponse<List<PtItemDto>> saveItems(@PathVariable("sectionCd") String sectionCd, @RequestBody List<PtItemDto> body,
                                                  HttpServletRequest request) {
        return ApiResponse.success("상품 목록이 저장되었습니다.", adminService.saveSectionItems(sectionCd, body, auth.requireAdmin(request)));
    }

    @GetMapping("/products")
    public ApiResponse<List<PtProductDto>> products(HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(adminService.products());
    }

    // ---------------------------------------------------------------- 미리보기
    @GetMapping("/preview")
    public ApiResponse<ProductTabView> preview(@RequestParam(value = "channel", defaultValue = "APP") String channel,
                                               @RequestParam(value = "baseDt", required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDt,
                                               HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(baseDt == null ? service.getView(channel) : service.getView(channel, baseDt));
    }
}
