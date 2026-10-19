package com.example.demojsp.common.notice.mapper;

import com.example.demojsp.common.notice.dto.NoticeBizDto;
import com.example.demojsp.common.notice.dto.NoticeDto;
import com.example.demojsp.common.notice.dto.NoticeTargetDto;
import com.example.demojsp.common.notice.dto.NoticeTypeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** [공통#28] 공지사항 - SQL 은 resources/mapper/common/NoticeMapper.xml */
@Mapper
public interface NoticeMapper {

    // ===================================================== 사용자 화면 (게시 중 공지만)
    /** 목록 (scope/target/type 이 null 이면 조건 없음) - 상단고정 → 게시 시작 최신순 */
    List<NoticeDto> selectPostedList(@Param("scopeCd") String scopeCd, @Param("targetCd") String targetCd,
                                     @Param("typeCd") String typeCd, @Param("now") LocalDateTime now,
                                     @Param("offset") int offset, @Param("limit") int limit);

    int countPosted(@Param("scopeCd") String scopeCd, @Param("targetCd") String targetCd,
                    @Param("typeCd") String typeCd, @Param("now") LocalDateTime now);

    /** 메인 띠: STRIP_YN='Y' 중 게시 시작이 가장 최근인 1건 */
    NoticeDto selectStrip(@Param("now") LocalDateTime now);

    /** 메인 팝업: POPUP_YN='Y' (상단고정 → 최신순) */
    List<NoticeDto> selectPopups(@Param("now") LocalDateTime now);

    /** 상세 (게시 중일 때만) */
    NoticeDto selectPosted(@Param("noticeId") long noticeId, @Param("now") LocalDateTime now);

    int increaseViewCnt(@Param("noticeId") long noticeId);

    /** 필터 칩: 게시 중 공지가 있는 상품/업무 + 건수 */
    List<NoticeTargetDto> selectPostedTargets(@Param("now") LocalDateTime now);

    // ===================================================== ADMIN
    List<NoticeDto> selectAdminList(@Param("scopeCd") String scopeCd, @Param("typeCd") String typeCd,
                                    @Param("keyword") String keyword, @Param("limit") int limit);

    NoticeDto selectNotice(@Param("noticeId") long noticeId);

    int insertNotice(NoticeDto notice);

    int updateNotice(NoticeDto notice);

    List<NoticeTypeDto> selectTypes();

    NoticeTypeDto selectType(@Param("typeCd") String typeCd);

    int mergeType(NoticeTypeDto type);

    List<NoticeBizDto> selectBizs();

    NoticeBizDto selectBiz(@Param("bizCd") String bizCd);

    int mergeBiz(NoticeBizDto biz);

    /** 상품별 공지 대상 = 상품 원장 (TB_PRODUCT) */
    List<NoticeTargetDto> selectProductTargets();
}
