package com.example.demojsp.common.platformlink.adapter;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** JSON 전문 제휴사 어댑터 공통 도우미 (필수값 읽기 · 오류 메시지 통일) */
public abstract class JsonPartnerAdapter implements PartnerAdapter {

    protected final ObjectMapper om;

    protected JsonPartnerAdapter(ObjectMapper om) {
        this.om = om;
    }

    protected JsonNode tree(String body, String reqNoField) {
        try {
            JsonNode n = om.readTree(body == null ? "" : body);
            if (n == null || !n.isObject()) throw new AdapterParseException("JSON 객체가 아닙니다.", null);
            return n;
        } catch (JacksonException e) {
            throw new AdapterParseException("JSON 형식 오류", null);
        }
    }

    /** 필수 문자열 (점 경로: "applicant.name") */
    protected String req(JsonNode root, String path, String reqNo) {
        String v = opt(root, path);
        if (v == null) throw new AdapterParseException("필수값 누락: " + path, reqNo);
        return v;
    }

    protected String opt(JsonNode root, String path) {
        JsonNode n = root;
        for (String p : path.split("\\.")) n = n.path(p);
        if (n.isMissingNode() || n.isNull()) return null;
        String v = n.asString("").strip();
        return v.isEmpty() ? null : v;
    }

    protected long num(JsonNode root, String path, String reqNo, boolean required) {
        String v = required ? req(root, path, reqNo) : opt(root, path);
        if (v == null) return 0;
        try {
            return Long.parseLong(v.replace(",", ""));
        } catch (NumberFormatException e) {
            throw new AdapterParseException("숫자 형식 오류: " + path, reqNo);
        }
    }

    protected String write(ObjectNode n) {
        return om.writeValueAsString(n);
    }

    protected static String digits8(String v, String reqNo, String path) {
        String d = v == null ? "" : v.replaceAll("[^0-9]", "");
        if (d.length() != 8) throw new AdapterParseException("날짜 형식 오류(yyyyMMdd): " + path, reqNo);
        return d;
    }
}
