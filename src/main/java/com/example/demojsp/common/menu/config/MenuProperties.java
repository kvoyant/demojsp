package com.example.demojsp.common.menu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [공통#46] 홈 퀵메뉴 · 전체메뉴(뱅킹메뉴) 설정 ({@code common.menu.*}).
 *
 * <p>퀵메뉴는 요구사항상 <b>하드코딩</b>(ADMIN 관리 아님) → 코드가 아니라 yaml 에 고정 정의해 배포로만 바뀌게 한다.
 * 뱅킹메뉴(전체메뉴)는 ADMIN "뱅킹메뉴목록" 데이터 → seed 는 운영 DB 를 흉내낸 초기값.
 */
@ConfigurationProperties(prefix = "common.menu")
public class MenuProperties {

    /** ADMIN API 개방 (local 전용, 운영 false = ROLE_ADMIN 세션 필요) */
    private boolean adminApiOpen = false;
    /** 로그인 화면 URL (비로그인 고객이 로그인 필요 메뉴를 누르면 이동) */
    private String loginUrl = "/login";
    private Quick quick = new Quick();
    private List<BankingSeed> bankingSeed = new ArrayList<>();

    public boolean isAdminApiOpen() { return adminApiOpen; }
    public void setAdminApiOpen(boolean adminApiOpen) { this.adminApiOpen = adminApiOpen; }
    public String getLoginUrl() { return loginUrl; }
    public void setLoginUrl(String loginUrl) { this.loginUrl = loginUrl; }
    public Quick getQuick() { return quick; }
    public void setQuick(Quick quick) { this.quick = quick; }
    public List<BankingSeed> getBankingSeed() { return bankingSeed; }
    public void setBankingSeed(List<BankingSeed> bankingSeed) { this.bankingSeed = bankingSeed; }

    /** 퀵메뉴 */
    public static class Quick {
        /** 노출 개수 (기능1: 6개) */
        private int maxCount = 6;
        /** 열 수 (기능1: 3*2 배열) */
        private int columns = 3;
        /** 메뉴 정의 (기능3 하드코딩 메뉴) */
        private List<QuickItem> items = new ArrayList<>();
        /**
         * 회원구분별 배치 (정책2 사용자 유형별 개인화). 키: DEFAULT / GUEST / MEMBER / FINANCIAL, 값: 메뉴 코드 순서.
         * 구분 키가 없으면 DEFAULT 를 쓴다.
         */
        private Map<String, List<String>> layouts = new LinkedHashMap<>();

        public int getMaxCount() { return maxCount; }
        public void setMaxCount(int maxCount) { this.maxCount = maxCount; }
        public int getColumns() { return columns; }
        public void setColumns(int columns) { this.columns = columns; }
        public List<QuickItem> getItems() { return items; }
        public void setItems(List<QuickItem> items) { this.items = items; }
        public Map<String, List<String>> getLayouts() { return layouts; }
        public void setLayouts(Map<String, List<String>> layouts) { this.layouts = layouts; }
    }

    /** 퀵메뉴 1개 (기능2 구조: 이미지 + 메뉴명) */
    public static class QuickItem {
        private String code;
        private String name;
        /** 공통 아이콘(공통#41) 이름 - /images/common/ui-icons.svg#ic-{icon} */
        private String icon;
        private String url;
        /** 로그인 필요 메뉴 → 비로그인 고객은 로그인 후 이동 */
        private boolean requiresLogin;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public boolean isRequiresLogin() { return requiresLogin; }
        public void setRequiresLogin(boolean requiresLogin) { this.requiresLogin = requiresLogin; }
    }

    /** 뱅킹메뉴 초기 데이터 */
    public static class BankingSeed {
        private String category;
        private String categoryName;
        private String name;
        private String url;
        private int displayOrder = 1;
        private boolean use = true;
        /** 신규 메뉴 표시 (전체메뉴 "N" 배지) */
        private boolean newBadge;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public int getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
        public boolean isUse() { return use; }
        public void setUse(boolean use) { this.use = use; }
        public boolean isNewBadge() { return newBadge; }
        public void setNewBadge(boolean newBadge) { this.newBadge = newBadge; }
    }
}
