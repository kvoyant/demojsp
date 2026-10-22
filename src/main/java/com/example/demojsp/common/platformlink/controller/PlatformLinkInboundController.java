package com.example.demojsp.common.platformlink.controller;

import com.example.demojsp.common.platformlink.security.PartnerSignature;
import com.example.demojsp.common.platformlink.service.PlatformLinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * [제휴사 → 당행] 한도조회 수신 창구 (Inbound). 제휴사 서버가 직접 호출한다 - 고객 화면용 API 아님.
 *
 * <pre>
 *  POST /api/link/platform/{partnerCd}/limit
 *  Header  X-Link-Timestamp, X-Link-Signature
 *  Body    제휴사 형식 JSON (그대로 받는다 - 형식 해석은 어댑터 몫)
 * </pre>
 * 본문을 String 으로 받는 이유: ① 서명은 "받은 바이트 그대로" 검증해야 하고 ② 제휴사마다 형식이 달라 DTO 하나로 받을 수 없다.
 * 운영: 로그인 인터셉터 대상 아님(세션 없음). 로드밸런서 뒤라면 실제 송신 IP 를 X-Forwarded-For 로 받도록 서버 설정
 * (server.forward-headers-strategy) 후 허용 IP 를 검사한다.
 */
@RestController
public class PlatformLinkInboundController {

    private final PlatformLinkService service;

    public PlatformLinkInboundController(PlatformLinkService service) {
        this.service = service;
    }

    @PostMapping(value = "/api/link/platform/{partnerCd}/limit")
    public ResponseEntity<String> limit(@PathVariable("partnerCd") String partnerCd,
                                        @RequestHeader(value = PartnerSignature.H_TIMESTAMP, required = false) String timestamp,
                                        @RequestHeader(value = PartnerSignature.H_SIGNATURE, required = false) String signature,
                                        @RequestBody(required = false) String body, HttpServletRequest request) {
        PlatformLinkService.LinkResponse r = service.handle(partnerCd, timestamp, signature, body, request.getRemoteAddr());
        return ResponseEntity.status(r.httpStatus()).contentType(MediaType.APPLICATION_JSON).body(r.body());
    }
}
