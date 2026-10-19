package com.example.demojsp.common.banner.store;

import com.example.demojsp.common.banner.domain.BannerCategory;
import com.example.demojsp.common.banner.domain.BannerChannel;
import com.example.demojsp.common.banner.domain.BannerPopup;
import com.example.demojsp.common.banner.domain.BannerPopupType;
import com.example.demojsp.common.banner.mapper.BannerPopupMapper;
import com.example.demojsp.common.banner.mapper.BannerPopupRow;
import com.example.demojsp.common.store.CommonSequence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 배너팝업목록 DB 저장소 (TB_BANNER_POPUP, MyBatis) - {@code common.store.type=db}(기본).
 * 노출 채널은 쉼표 구분 문자열(APP,PC_WEB)로 저장한다.
 */
@org.springframework.stereotype.Repository
@ConditionalOnProperty(name = "common.store.type", havingValue = "db", matchIfMissing = true)
public class MyBatisBannerPopupRepository implements BannerPopupRepository {

    static final String SEQ = "BANNER_POPUP";

    private final BannerPopupMapper mapper;
    private final CommonSequence sequence;

    public MyBatisBannerPopupRepository(BannerPopupMapper mapper, CommonSequence sequence) {
        this.mapper = mapper;
        this.sequence = sequence;
    }

    @Override
    public List<BannerPopup> findAll() {
        return mapper.selectAll().stream().map(MyBatisBannerPopupRepository::toDomain).toList();
    }

    @Override
    public Optional<BannerPopup> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(MyBatisBannerPopupRepository::toDomain);
    }

    @Override
    public long nextId() {
        return sequence.next(SEQ, 1000);       // 메모리 저장소와 같은 번호대 (1001~)
    }

    @Override
    public void save(BannerPopup banner) {
        BannerPopupRow row = toRow(banner);
        if (mapper.update(row) == 0) {
            mapper.insert(row);
        }
    }

    @Override
    public boolean delete(long id) {
        return mapper.delete(id) > 0;
    }

    static BannerPopup toDomain(BannerPopupRow r) {
        List<BannerChannel> channels = Arrays.stream(r.getChannels().split(","))
                .map(String::strip).filter(s -> !s.isEmpty()).map(BannerChannel::valueOf).toList();
        return new BannerPopup(r.getBannerId(), BannerPopupType.valueOf(r.getPopupType()), BannerCategory.valueOf(r.getCategory()),
                channels, r.getTitle(), r.getContent(), r.getImageUrl(), r.getAltText(), r.getLinkUrl(), r.getBgColor(),
                r.getStartAt(), r.getEndAt(), r.getDisplayOrder(), "Y".equals(r.getUseYn()),
                r.getRegAt(), r.getRegBy(), r.getUpdAt(), r.getUpdBy());
    }

    static BannerPopupRow toRow(BannerPopup b) {
        BannerPopupRow r = new BannerPopupRow();
        r.setBannerId(b.id());
        r.setPopupType(b.popupType().name());
        r.setCategory(b.category().name());
        r.setChannels(b.channels().stream().map(Enum::name).collect(Collectors.joining(",")));
        r.setTitle(b.title());
        r.setContent(b.content());
        r.setImageUrl(b.imageUrl());
        r.setAltText(b.altText());
        r.setLinkUrl(b.linkUrl());
        r.setBgColor(b.backgroundColor());
        r.setStartAt(b.startAt());
        r.setEndAt(b.endAt());
        r.setDisplayOrder(b.displayOrder());
        r.setUseYn(b.use() ? "Y" : "N");
        r.setRegAt(b.registeredAt());
        r.setRegBy(b.registeredBy());
        r.setUpdAt(b.updatedAt());
        r.setUpdBy(b.updatedBy());
        return r;
    }
}
