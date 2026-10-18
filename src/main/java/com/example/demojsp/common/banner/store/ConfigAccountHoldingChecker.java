package com.example.demojsp.common.banner.store;

import com.example.demojsp.common.banner.config.BannerPopupProperties;
import com.example.demojsp.common.banner.spi.AccountHoldingChecker;
import org.springframework.stereotype.Component;

/**
 * 계좌 보유 여부 - local 흉내 구현 (yaml {@code common.banner-popup.top-slide.account-holders}).
 * 운영에서는 계좌원장 조회 구현체로 교체한다.
 */
@Component
public class ConfigAccountHoldingChecker implements AccountHoldingChecker {

    private final BannerPopupProperties properties;

    public ConfigAccountHoldingChecker(BannerPopupProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean hasAccount(String customerId) {
        return customerId != null && properties.getTopSlide().getAccountHolders().contains(customerId);
    }
}
