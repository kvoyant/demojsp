package com.example.demojsp.domain.login.service;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * =========================================================================================
 * [금융권 중복 로그인 방지 관리자 (Concurrent Session Control)]
 * 
 * - 동일한 회원 ID로 다른 브라우저 또는 기기에서 중복 로그인 시,
 *   기존 활성 세션을 즉시 만료시키고 "다른 기기에서 로그인되었습니다" 알림을 발생시킵니다.
 * =========================================================================================
 */
@Slf4j
@Component
public class ConcurrentSessionManager {

    private final Map<String, HttpSession> activeSessionMap = new ConcurrentHashMap<>();

    /**
     * 신규 세션 등록 및 기존 세션 강제 만료
     */
    public void registerSession(String memberId, HttpSession newSession) {
        HttpSession oldSession = activeSessionMap.get(memberId);
        if (oldSession != null && !oldSession.getId().equals(newSession.getId())) {
            log.warn("[중복 로그인 감지] 기존 세션 만료 처리: memberId={}, oldSessionId={}", memberId, oldSession.getId());
            try {
                oldSession.setAttribute("EXPIRED_BY_CONCURRENT", true);
                oldSession.invalidate();
            } catch (IllegalStateException e) {
                // 이미 만료된 세션 무시
            }
        }
        activeSessionMap.put(memberId, newSession);
    }

    /**
     * 로그아웃 또는 세션 종료 시 제거
     */
    public void removeSession(String memberId) {
        activeSessionMap.remove(memberId);
    }
}
