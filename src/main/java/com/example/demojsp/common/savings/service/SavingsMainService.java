package com.example.demojsp.common.savings.service;

import com.example.demojsp.common.member.MemberContext;
import com.example.demojsp.common.member.MemberSegment;
import com.example.demojsp.common.product.domain.Product;
import com.example.demojsp.common.product.domain.ProductKind;
import com.example.demojsp.common.product.store.ProductRepository;
import com.example.demojsp.common.savings.config.SavingsProperties;
import com.example.demojsp.common.savings.domain.SavingsMainView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * [공통#51] 저축 - 메인 콘텐츠.
 *
 * <ul>
 *   <li>방향: 계좌 미보유 고객에게 계좌개설 프로세스 제공 / 목적: 입출금통장 개설 촉진</li>
 *   <li>정책1 제공대상: 계좌를 보유하지 않은 모든 사용자 (비로그인 포함) / 정책2 로그인 전·후 같은 화면</li>
 *   <li>기능1 구조: 텍스트 + 이미지 + 버튼 / 기능2 "매일 쌓이는 이자 / 모으는 재미가 쏠쏠"</li>
 *   <li>기능3 [계좌 개설하기] 하단 상시 노출, 터치 → 입출금통장 상품안내페이지 (상품 원장 URL)</li>
 * </ul>
 * 계좌 보유 고객(금융회원)은 eligible=false → 같은 자리에 계좌정보 (상단슬라이드배너 공통#44 와 같은 규칙).
 */
@Service
public class SavingsMainService {

    private static final Logger log = LoggerFactory.getLogger(SavingsMainService.class);

    private final SavingsProperties properties;
    private final ProductRepository productRepository;

    public SavingsMainService(SavingsProperties properties, ProductRepository productRepository) {
        this.properties = properties;
        this.productRepository = productRepository;
    }

    public SavingsMainView main(MemberContext member) {
        SavingsProperties.Main m = properties.getMain();
        boolean eligible = member.segment() != MemberSegment.FINANCIAL;
        if (!eligible) {
            return new SavingsMainView(false, member.segment().name(), List.of(), null, null, null, null, null);
        }
        Product product = productRepository.findAll().stream()
                .filter(p -> p.kind() == ProductKind.DEPOSIT && p.code().equals(m.getProductCode()))
                .findFirst().orElse(null);
        String url = m.getFallbackUrl();
        String name = null;
        if (product != null && product.onSale()) {
            url = product.detailUrl();
            name = product.name();
        } else {
            log.warn("[저축 메인] 입출금통장 상품 {} 없음/판매중지 → 대체 URL {}", m.getProductCode(), url);
        }
        // 정책2: 로그인 전(GUEST)·후(MEMBER) 같은 내용
        return new SavingsMainView(true, member.segment().name(), List.copyOf(m.getLines()), m.getImageUrl(), m.getImageAlt(),
                m.getCtaText(), url, name);
    }
}
