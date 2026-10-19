package com.example.demojsp.common.store;

/**
 * DB 저장소일 때 기동 후 "테이블이 비어 있으면 seed 적재" 를 하는 공통모듈 (이어하기·배너·뱅킹메뉴·상품).
 * 메모리 저장소는 각 서비스 생성자에서 바로 적재한다.
 */
public interface CommonSeeder {

    /** 로그용 이름 */
    String seedName();

    /** 비어 있을 때만 seed 적재 */
    void seedIfEmpty();
}
