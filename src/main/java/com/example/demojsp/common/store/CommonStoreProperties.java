package com.example.demojsp.common.store;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공통모듈 저장소 설정 ({@code common.store.*}).
 *
 * <ul>
 *   <li>{@code type=db} (기본) - 이어하기·배너·뱅킹메뉴·상품 데이터를 MySQL(demo_db02) 테이블에 저장 (MyBatis)</li>
 *   <li>{@code type=memory} - 메모리 저장 (DB 없이 화면만 볼 때, 재기동 시 초기화)</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "common.store")
public class CommonStoreProperties {

    /** db | memory */
    private String type = "db";
    /** 기동 시 schema-common-home.sql 실행 (CREATE TABLE IF NOT EXISTS - 기존 데이터 유지). 운영은 false 로 두고 DBA 가 DDL 적용 */
    private boolean initSchema = true;
    /** 기동 시 테이블이 비어 있으면 yaml seed 를 넣음 (local 샘플 데이터). 운영 false */
    private boolean seedIfEmpty = true;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isInitSchema() { return initSchema; }
    public void setInitSchema(boolean initSchema) { this.initSchema = initSchema; }
    public boolean isSeedIfEmpty() { return seedIfEmpty; }
    public void setSeedIfEmpty(boolean seedIfEmpty) { this.seedIfEmpty = seedIfEmpty; }
}
