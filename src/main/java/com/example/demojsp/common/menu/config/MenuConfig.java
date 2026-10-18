package com.example.demojsp.common.menu.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** [공통#46] 퀵메뉴·뱅킹메뉴 설정 등록 */
@Configuration
@EnableConfigurationProperties(MenuProperties.class)
public class MenuConfig {
}
