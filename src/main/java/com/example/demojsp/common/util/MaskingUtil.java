package com.example.demojsp.common.util;

import org.springframework.util.StringUtils;

public class MaskingUtil {

    /**
     * 주민등록번호 마스킹 (860101-1******)
     */
    public static String maskRrn(String rrn) {
        if (!StringUtils.hasText(rrn)) return "";
        String cleanRrn = rrn.replaceAll("-", "");
        if (cleanRrn.length() == 13) {
            return cleanRrn.substring(0, 6) + "-" + cleanRrn.charAt(6) + "******";
        }
        return rrn;
    }

    /**
     * 이름 마스킹 (홍*동, 신**기)
     */
    public static String maskName(String name) {
        if (!StringUtils.hasText(name)) return "";
        int len = name.length();
        if (len == 2) {
            return name.charAt(0) + "*";
        } else if (len > 2) {
            return name.charAt(0) + "*".repeat(len - 2) + name.charAt(len - 1);
        }
        return name;
    }

    /**
     * 전화번호 마스킹 (010-****-1234)
     */
    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) return "";
        String cleanPhone = phone.replaceAll("-", "");
        if (cleanPhone.length() == 11) {
            return cleanPhone.substring(0, 3) + "-****-" + cleanPhone.substring(7);
        } else if (cleanPhone.length() == 10) {
            return cleanPhone.substring(0, 3) + "-***-" + cleanPhone.substring(6);
        }
        return phone;
    }

    /**
     * 계좌번호 마스킹 (뒤 4자리 제외 마스킹)
     */
    public static String maskAccount(String accountNo) {
        if (!StringUtils.hasText(accountNo)) return "";
        int len = accountNo.length();
        if (len > 4) {
            return "*".repeat(len - 4) + accountNo.substring(len - 4);
        }
        return accountNo;
    }
}
