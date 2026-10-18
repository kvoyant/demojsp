package com.example.demojsp.common.menu.store;

import com.example.demojsp.common.menu.domain.BankingMenu;

import java.util.List;
import java.util.Optional;

/**
 * 뱅킹메뉴 저장소 (ADMIN 뱅킹메뉴목록). 운영은 기존 메뉴 테이블 MyBatis 구현체로 교체 ({@code @Primary}).
 */
public interface BankingMenuRepository {

    List<BankingMenu> findAll();

    Optional<BankingMenu> findById(long id);

    long nextId();

    void save(BankingMenu menu);
}
