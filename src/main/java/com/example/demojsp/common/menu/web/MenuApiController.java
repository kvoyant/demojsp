package com.example.demojsp.common.menu.web;

import com.example.demojsp.common.member.MemberSegmentResolver;
import com.example.demojsp.common.menu.domain.AllMenuView;
import com.example.demojsp.common.menu.domain.QuickMenuView;
import com.example.demojsp.common.menu.service.BankingMenuService;
import com.example.demojsp.common.menu.service.QuickMenuService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [공통#46] 고객 메뉴 API (로그인 불필요).
 *
 * <pre>
 *  GET /api/common/menus/quick   홈 퀵메뉴 (회원구분별 6개, 3*2)
 *  GET /api/common/menus/all     전체메뉴 (ADMIN 뱅킹메뉴목록 기준)
 * </pre>
 */
@RestController
@RequestMapping("/api/common/menus")
public class MenuApiController {

    private final QuickMenuService quickMenuService;
    private final BankingMenuService bankingMenuService;
    private final MemberSegmentResolver memberResolver;

    public MenuApiController(QuickMenuService quickMenuService, BankingMenuService bankingMenuService,
                             MemberSegmentResolver memberResolver) {
        this.quickMenuService = quickMenuService;
        this.bankingMenuService = bankingMenuService;
        this.memberResolver = memberResolver;
    }

    @GetMapping("/quick")
    public ApiResponse<QuickMenuView> quick(HttpServletRequest request) {
        return ApiResponse.success(quickMenuService.quickMenu(memberResolver.resolve(request)));
    }

    @GetMapping("/all")
    public ApiResponse<AllMenuView> all() {
        return ApiResponse.success(bankingMenuService.allMenu());
    }
}
