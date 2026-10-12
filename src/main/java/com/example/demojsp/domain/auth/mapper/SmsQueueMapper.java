package com.example.demojsp.domain.auth.mapper;

import com.example.demojsp.domain.auth.client.dto.SmsQueueDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SmsQueueMapper {

    /**
     * 금융 SMS 발송 큐에 등록 (DB 인터페이스 방식)
     */
    int insertSmsQueue(SmsQueueDto queueDto);

    /**
     * 발송 상태 업데이트
     */
    int updateSmsStatus(@Param("smsSeq") Long smsSeq,
                        @Param("sendStatus") String sendStatus,
                        @Param("extResultCode") String extResultCode);

    /**
     * 트랜잭션별 SMS 발송 이력 조회
     */
    List<SmsQueueDto> selectSmsListByTxSeq(@Param("reqTxSeq") String reqTxSeq);
}
