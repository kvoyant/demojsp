package com.example.demojsp.common.notice.service;

import com.example.demojsp.common.banner.service.BannerSupport;
import com.example.demojsp.common.notice.dto.NoticeBizDto;
import com.example.demojsp.common.notice.dto.NoticeDto;
import com.example.demojsp.common.notice.dto.NoticeFilters;
import com.example.demojsp.common.notice.dto.NoticeTargetDto;
import com.example.demojsp.common.notice.dto.NoticeTypeDto;
import com.example.demojsp.common.notice.exception.NoticeErrorCode;
import com.example.demojsp.common.notice.exception.NoticeException;
import com.example.demojsp.common.notice.mapper.NoticeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class NoticeAdminServiceImpl implements NoticeAdminService {

    /** 유형코드·업무코드: 영문 대문자로 시작, 대문자·숫자·밑줄 2~20자 */
    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,19}$");

    private final NoticeMapper mapper;
    private Clock clock = Clock.systemDefaultZone();

    public NoticeAdminServiceImpl(NoticeMapper mapper) {
        this.mapper = mapper;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    // ================================================================ 공지

    @Override
    public List<NoticeDto> list(String scopeCd, String typeCd, String keyword, int limit) {
        List<NoticeDto> list = mapper.selectAdminList(NoticeServiceImpl.blankToNull(scopeCd), NoticeServiceImpl.blankToNull(typeCd),
                NoticeServiceImpl.blankToNull(keyword), Math.max(1, Math.min(limit, 500)));
        LocalDateTime now = LocalDateTime.now(clock);
        list.forEach(n -> n.setStatus(status(n, now)));
        return list;
    }

    @Override
    public NoticeDto get(long noticeId) {
        NoticeDto n = mapper.selectNotice(noticeId);
        if (n == null) throw new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND, "공지사항을 찾을 수 없습니다.");
        n.setStatus(status(n, LocalDateTime.now(clock)));
        return n;
    }

    @Override
    @Transactional
    public NoticeDto save(NoticeDto n, String adminId) {
        if (n == null) throw new NoticeException(NoticeErrorCode.INVALID_REQUEST);

        // ---- 요구사항 1: 전체 / 상품별 / 업무별 구분 - 범위에 맞는 대상만 허용
        if (n.getScopeCd() == null || !NoticeServiceImpl.SCOPES.contains(n.getScopeCd())) {
            throw invalid("구분(전체 / 상품별 / 업무별)을 선택하세요.");
        }
        String target = NoticeServiceImpl.blankToNull(n.getTargetCd());
        switch (n.getScopeCd()) {
            case NoticeDto.SCOPE_ALL -> target = null;
            case NoticeDto.SCOPE_PRODUCT -> {
                String t = target;
                if (t == null || mapper.selectProductTargets().stream().noneMatch(p -> p.getTargetCd().equals(t))) {
                    throw invalid("상품별 공지는 대상 상품을 선택하세요.");
                }
            }
            default -> {
                NoticeBizDto biz = target == null ? null : mapper.selectBiz(target);
                if (biz == null) throw invalid("업무별 공지는 대상 업무를 선택하세요.");
            }
        }
        n.setTargetCd(target);

        // ---- 요구사항 2: 공지 유형 (사용 중인 유형만 새로 고를 수 있다)
        NoticeTypeDto type = n.getTypeCd() == null ? null : mapper.selectType(n.getTypeCd());
        if (type == null) throw invalid("공지 유형을 선택하세요.");
        if (!"Y".equals(type.getUseYn())) {
            NoticeDto before = n.getNoticeId() == null ? null : mapper.selectNotice(n.getNoticeId());
            if (before == null || !type.getTypeCd().equals(before.getTypeCd())) {
                throw invalid("사용하지 않는 공지 유형입니다. (" + type.getTypeNm() + ")");
            }
        }

        // ---- 내용 · 게시기간 · 노출 옵션
        n.setTitle(text(n.getTitle(), "제목", 100, true));
        n.setContent(text(n.getContent(), "내용", 20_000, true));
        if (n.getStartAt() == null) throw invalid("게시 시작일시를 입력하세요.");
        if (n.getEndAt() != null && !n.getEndAt().isAfter(n.getStartAt())) throw invalid("게시 종료일시는 시작일시 이후여야 합니다.");
        n.setFixYn(yn(n.getFixYn(), "N", "상단 고정"));
        n.setStripYn(yn(n.getStripYn(), "N", "메인 띠 노출"));
        n.setPopupYn(yn(n.getPopupYn(), "N", "메인 팝업"));
        n.setUseYn(yn(n.getUseYn(), "Y", "사용여부"));
        n.setUpdAt(now());
        n.setUpdBy(adminId);

        if (n.getNoticeId() == null) {
            mapper.insertNotice(n);
        } else if (mapper.updateNotice(n) == 0) {
            throw new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND, "공지사항을 찾을 수 없습니다.");
        }
        return get(n.getNoticeId());
    }

    /** ADMIN 목록 상태 표시 */
    static String status(NoticeDto n, LocalDateTime now) {
        if (!"Y".equals(n.getUseYn())) return "미사용";
        if (n.getStartAt().isAfter(now)) return "게시예약";
        if (n.getEndAt() != null && !n.getEndAt().isAfter(now)) return "게시종료";
        return "게시중";
    }

    // ================================================================ 유형 · 업무 구분

    @Override
    public List<NoticeTypeDto> types() {
        return mapper.selectTypes();
    }

    @Override
    @Transactional
    public NoticeTypeDto saveType(NoticeTypeDto t, String adminId) {
        if (t == null) throw new NoticeException(NoticeErrorCode.INVALID_REQUEST);
        code(t.getTypeCd(), "유형코드");
        t.setTypeNm(text(t.getTypeNm(), "유형명", 20, true));
        if (!BannerSupport.isHexColor(t.getBadgeColor())) throw invalid("표시 색은 #RRGGBB 형식입니다.");
        t.setBadgeColor(t.getBadgeColor().toUpperCase());
        t.setSortOrd(order(t.getSortOrd()));
        t.setUseYn(yn(t.getUseYn(), "Y", "사용여부"));
        t.setUpdAt(now());
        t.setUpdBy(adminId);
        mapper.mergeType(t);
        return mapper.selectType(t.getTypeCd());
    }

    @Override
    public List<NoticeBizDto> bizs() {
        return mapper.selectBizs();
    }

    @Override
    @Transactional
    public NoticeBizDto saveBiz(NoticeBizDto b, String adminId) {
        if (b == null) throw new NoticeException(NoticeErrorCode.INVALID_REQUEST);
        code(b.getBizCd(), "업무코드");
        b.setBizNm(text(b.getBizNm(), "업무명", 30, true));
        b.setSortOrd(order(b.getSortOrd()));
        b.setUseYn(yn(b.getUseYn(), "Y", "사용여부"));
        b.setUpdAt(now());
        b.setUpdBy(adminId);
        mapper.mergeBiz(b);
        return mapper.selectBiz(b.getBizCd());
    }

    @Override
    public NoticeFilters targets() {
        NoticeFilters f = new NoticeFilters();
        f.setProducts(mapper.selectProductTargets());
        for (NoticeBizDto b : mapper.selectBizs()) {
            if (!"Y".equals(b.getUseYn())) continue;
            NoticeTargetDto t = new NoticeTargetDto();
            t.setScopeCd(NoticeDto.SCOPE_BIZ);
            t.setTargetCd(b.getBizCd());
            t.setTargetNm(b.getBizNm());
            f.getBizs().add(t);
        }
        return f;
    }

    // ================================================================ 검증 도우미

    private static void code(String v, String label) {
        if (v == null || !CODE.matcher(v).matches()) throw invalid(label + "는 영문 대문자로 시작하는 대문자·숫자·_ 2~20자로 입력하세요.");
    }

    private static int order(Integer v) {
        if (v == null || v < 1 || v > 99) throw invalid("표시순서는 1~99 사이 숫자로 입력하세요.");
        return v;
    }

    private static String text(String value, String label, int max, boolean required) {
        String v = value == null ? null : value.strip();
        if (v == null || v.isEmpty()) {
            if (required) throw invalid(label + "을(를) 입력하세요.");
            return null;
        }
        if (v.length() > max) throw invalid(label + "은(는) " + max + "자 이내로 입력하세요.");
        return v;
    }

    private static String yn(String value, String defaultValue, String label) {
        String v = value == null ? defaultValue : value;
        if (!"Y".equals(v) && !"N".equals(v)) throw invalid(label + "는 Y 또는 N 입니다.");
        return v;
    }

    private static NoticeException invalid(String message) {
        return new NoticeException(NoticeErrorCode.INVALID_VALUE, message);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
