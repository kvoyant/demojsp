package com.example.demojsp.common.savings.web;

import com.example.demojsp.common.member.MemberSegmentResolver;
import com.example.demojsp.common.response.ApiResponse;
import com.example.demojsp.common.savings.domain.SavingsMainView;
import com.example.demojsp.common.savings.service.SavingsMainService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [공통#51] 저축 탭 메인 콘텐츠 API (로그인 불필요).
 *
 * <pre>
 *  GET /api/common/savings/main   → eligible(계좌 미보유), 텍스트·이미지·버튼(문구·입출금통장 상품안내 URL)
 * </pre>
 */
@RestController
public class SavingsApiController {

    private final SavingsMainService service;
    private final MemberSegmentResolver memberResolver;

    public SavingsApiController(SavingsMainService service, MemberSegmentResolver memberResolver) {
        this.service = service;
        this.memberResolver = memberResolver;
    }

    @GetMapping("/api/common/savings/main")
    public ApiResponse<SavingsMainView> main(HttpServletRequest request) {
        return ApiResponse.success(service.main(memberResolver.resolve(request)));
    }
}
