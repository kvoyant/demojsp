package com.example.demojsp.common.notice.service;

import com.example.demojsp.common.notice.config.NoticeProperties;
import com.example.demojsp.common.notice.dto.NoticeDto;
import com.example.demojsp.common.notice.dto.NoticeFilters;
import com.example.demojsp.common.notice.dto.NoticePage;
import com.example.demojsp.common.notice.dto.NoticeTargetDto;
import com.example.demojsp.common.notice.exception.NoticeErrorCode;
import com.example.demojsp.common.notice.exception.NoticeException;
import com.example.demojsp.common.notice.mapper.NoticeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class NoticeServiceImpl implements NoticeService {

    static final Set<String> SCOPES = Set.of(NoticeDto.SCOPE_ALL, NoticeDto.SCOPE_PRODUCT, NoticeDto.SCOPE_BIZ);

    private final NoticeMapper mapper;
    private final NoticeProperties properties;
    private Clock clock = Clock.systemDefaultZone();

    public NoticeServiceImpl(NoticeMapper mapper, NoticeProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    /** 테스트용 시계 교체 */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    @Override
    public NoticeDto strip() {
        return mapper.selectStrip(now());       // 없으면 null → 화면은 띠 영역을 숨긴다
    }

    @Override
    public NoticePage list(String scopeCd, String targetCd, String typeCd, int page) {
        String scope = blankToNull(scopeCd);
        if (scope != null && !SCOPES.contains(scope)) {
            throw new NoticeException(NoticeErrorCode.INVALID_REQUEST, "구분은 ALL(전체) / PRODUCT(상품별) / BIZ(업무별) 입니다.");
        }
        String target = NoticeDto.SCOPE_ALL.equals(scope) ? null : blankToNull(targetCd);   // 전체공지에는 대상이 없다
        String type = blankToNull(typeCd);
        int size = Math.max(1, properties.getPageSize());
        int p = Math.max(1, page);
        LocalDateTime now = now();
        int total = mapper.countPosted(scope, target, type, now);
        List<NoticeDto> items = mapper.selectPostedList(scope, target, type, now, (p - 1) * size, size);
        return new NoticePage(items, total, p, size);
    }

    @Override
    public NoticeFilters filters() {
        LocalDateTime now = now();
        NoticeFilters f = new NoticeFilters();
        f.setAllCnt(mapper.countPosted(null, null, null, now));
        for (NoticeTargetDto t : mapper.selectPostedTargets(now)) {
            (NoticeDto.SCOPE_PRODUCT.equals(t.getScopeCd()) ? f.getProducts() : f.getBizs()).add(t);
        }
        return f;
    }

    @Override
    @Transactional
    public NoticeDto detail(long noticeId) {
        NoticeDto n = mapper.selectPosted(noticeId, now());
        if (n == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND);   // 없는 번호 · 게시 전 · 게시 종료 · 미사용
        }
        mapper.increaseViewCnt(noticeId);
        n.setViewCnt(n.getViewCnt() + 1);
        return n;
    }

    @Override
    public List<NoticeDto> popups() {
        return mapper.selectPopups(now());
    }

    static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.strip();
    }
}
