package com.example.demojsp.common.menu.web;

import com.example.demojsp.common.menu.domain.BankingMenu;
import com.example.demojsp.common.menu.domain.BankingMenuRequest;
import com.example.demojsp.common.menu.exception.MenuErrorCode;
import com.example.demojsp.common.menu.service.BankingMenuService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * [공통#46 기능5] ADMIN 뱅킹메뉴목록 API.
 *
 * <pre>
 *  GET  /api/common/menu-admin/categories
 *  GET  /api/common/menu-admin/banking-menus?category=PRODUCT
 *  POST /api/common/menu-admin/banking-menus              신규등록 (예: 전체상품, 표시순서 1)
 *  PUT  /api/common/menu-admin/banking-menus/{id}
 *  POST /api/common/menu-admin/banking-menus/reorder?category=PRODUCT   body: [id, id, ...]
 * </pre>
 */
@RestController
@RequestMapping("/api/common/menu-admin")
public class BankingMenuAdminApiController {

    private final BankingMenuService service;
    private final MenuRequestContext context;

    public BankingMenuAdminApiController(BankingMenuService service, MenuRequestContext context) {
        this.service = service;
        this.context = context;
    }

    @GetMapping("/categories")
    public ApiResponse<Map<String, String>> categories(HttpServletRequest request) {
        context.requireAdmin(request);
        return ApiResponse.success(service.categories());
    }

    @GetMapping("/banking-menus")
    public ApiResponse<List<BankingMenu>> list(@RequestParam(value = "category", required = false) String category,
                                               HttpServletRequest request) {
        context.requireAdmin(request);
        return ApiResponse.success(service.adminList(category));
    }

    @PostMapping("/banking-menus")
    public ApiResponse<BankingMenu> register(@RequestBody BankingMenuRequest body, HttpServletRequest request) {
        return ApiResponse.success("뱅킹메뉴가 등록되었습니다.", service.register(body, context.requireAdmin(request)));
    }

    @PutMapping("/banking-menus/{id}")
    public ApiResponse<BankingMenu> update(@PathVariable("id") long id, @RequestBody BankingMenuRequest body,
                                           HttpServletRequest request) {
        return ApiResponse.success("뱅킹메뉴가 수정되었습니다.", service.update(id, body, context.requireAdmin(request)));
    }

    @PostMapping("/banking-menus/reorder")
    public ApiResponse<List<BankingMenu>> reorder(@RequestParam("category") String category, @RequestBody List<Long> ids,
                                                  HttpServletRequest request) {
        return ApiResponse.success("표시순서가 변경되었습니다.", service.reorder(category, ids, context.requireAdmin(request)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> notReadable(HttpMessageNotReadableException e) {
        MenuErrorCode c = MenuErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(c.getStatus()).body(ApiResponse.error(c.getCode(), c.getMessage()));
    }
}
