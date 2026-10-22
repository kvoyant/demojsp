package com.example.demojsp.common.platformlink.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * 처리 단계 기록 - 샘플 화면에서 "연계가 안에서 어떻게 흘러가는지" 보여 주기 위한 것.
 * 운영 응답에는 포함되지 않는다 (제휴사는 응답 전문만 받는다).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LinkTrace {

    private final long start = System.nanoTime();
    private final List<Step> steps = new ArrayList<>();
    private Object commonModel;
    private Object cssRequest;

    public void step(String name, boolean ok, String detail) {
        steps.add(new Step(name, ok, detail, (System.nanoTime() - start) / 1_000_000));
    }

    public long elapsedMs() {
        return (System.nanoTime() - start) / 1_000_000;
    }

    public List<Step> getSteps() { return steps; }
    public Object getCommonModel() { return commonModel; }
    public void setCommonModel(Object commonModel) { this.commonModel = commonModel; }
    public Object getCssRequest() { return cssRequest; }
    public void setCssRequest(Object cssRequest) { this.cssRequest = cssRequest; }

    /** @param atMs 요청 수신부터 이 단계가 끝날 때까지 누적 ms */
    public record Step(String name, boolean ok, String detail, long atMs) {
    }
}
