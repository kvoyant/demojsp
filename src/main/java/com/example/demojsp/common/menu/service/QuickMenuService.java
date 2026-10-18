package com.example.demojsp.common.menu.service;

import com.example.demojsp.common.member.MemberContext;
import com.example.demojsp.common.member.MemberSegment;
import com.example.demojsp.common.menu.config.MenuProperties;
import com.example.demojsp.common.menu.domain.QuickMenuItem;
import com.example.demojsp.common.menu.domain.QuickMenuView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [공통#46] 홈 퀵메뉴 - 회원구분 별 개인화 메뉴.
 *
 * <ul>
 *   <li>정책1 제공대상: 모든 사용자 (비로그인 포함)</li>
 *   <li>정책2 사용자 유형별 개인화: 회원구분(GUEST/MEMBER/FINANCIAL)별 배치({@code quick.layouts}),
 *       비로그인 고객이 로그인 필요 메뉴를 누르면 로그인 후 해당 메뉴로 이동</li>
 *   <li>기능1 6개 · 3*2 배열 / 기능2 이미지 + 메뉴명 / 기능3 하드코딩 6종 (yaml 고정, ADMIN 관리 아님)</li>
 * </ul>
 */
@Service
public class QuickMenuService {

    private static final Logger log = LoggerFactory.getLogger(QuickMenuService.class);
    static final String DEFAULT_LAYOUT = "DEFAULT";

    private final MenuProperties properties;
    private final Map<String, MenuProperties.QuickItem> byCode = new LinkedHashMap<>();

    public QuickMenuService(MenuProperties properties) {
        this.properties = properties;
        for (MenuProperties.QuickItem i : properties.getQuick().getItems()) {
            if (i.getCode() == null || i.getName() == null || i.getUrl() == null || !i.getUrl().startsWith("/") || i.getUrl().startsWith("//")) {
                log.warn("[퀵메뉴] 정의 오류로 제외 code={}", i.getCode());
                continue;
            }
            byCode.put(i.getCode(), i);
        }
    }

    public QuickMenuView quickMenu(MemberContext member) {
        MenuProperties.Quick q = properties.getQuick();
        List<String> layout = q.getLayouts().getOrDefault(member.segment().name(), q.getLayouts().get(DEFAULT_LAYOUT));
        if (layout == null) {
            layout = new ArrayList<>(byCode.keySet());       // 배치 미정의 → 정의 순서
        }
        List<QuickMenuItem> items = new ArrayList<>();
        for (String code : layout) {
            MenuProperties.QuickItem i = byCode.get(code);
            if (i == null) {
                log.warn("[퀵메뉴] 배치에 없는 메뉴 코드 {} (segment={})", code, member.segment());
                continue;
            }
            if (items.size() >= q.getMaxCount()) break;
            boolean loginFirst = i.isRequiresLogin() && member.segment() == MemberSegment.GUEST;
            String url = loginFirst
                    ? properties.getLoginUrl() + "?returnUrl=" + URLEncoder.encode(i.getUrl(), StandardCharsets.UTF_8)
                    : i.getUrl();
            items.add(new QuickMenuItem(i.getCode(), i.getName(), i.getIcon(), url, i.isRequiresLogin(), loginFirst));
        }
        int cols = Math.max(1, q.getColumns());
        return new QuickMenuView(member.segment().name(), member.segment().getLabel(), cols,
                (items.size() + cols - 1) / cols, items);
    }
}
