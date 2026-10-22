package com.example.demojsp.common.platformlink.adapter;

import com.example.demojsp.common.platformlink.model.LimitInquiry;
import com.example.demojsp.common.platformlink.model.LinkOutcome;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 토스형 전문 (TOSS_V1) - ※ 실제 토스 연동규격 수령 전 <b>가정</b> 형식. 규격을 받으면 이 클래스만 고친다.
 * <pre>
 * 요청 {"requestId":"T-001","applicant":{"ci":"..","name":"홍길동","birthDate":"1990-01-01"},
 *       "job":{"type":"EMPLOYEE|FREELANCER|SELF_EMPLOYED","companyName":".."},"income":{"annualIncome":50000000},
 *       "collateral":{"vehicleNumber":"12가3456"},"requestAmount":10000000}
 * 응답 {"requestId":"T-001","resultCode":"SUCCESS","resultMessage":"승인",
 *       "offer":{"productCode":"PEPPER-AUTO-01","productName":"페퍼오토론","limitAmount":30000000,"interestRate":9.9}}
 * </pre>
 * 특징: 날짜 yyyy-MM-dd, 금액 원, 금리 % 소수. 네이버파이낸셜(오픈 예정)도 같은 형식으로 가정해 이 어댑터를 공유한다.
 */
@Component
public class TossAdapter extends JsonPartnerAdapter {

    public TossAdapter(ObjectMapper om) {
        super(om);
    }

    @Override
    public String adapterCd() {
        return "TOSS_V1";
    }

    @Override
    public String description() {
        return "camelCase · 날짜 yyyy-MM-dd · 금액 원 · 금리 %(소수) · 담보는 collateral.vehicleNumber";
    }

    @Override
    public LimitInquiry parse(String body) {
        JsonNode r = tree(body, "requestId");
        String reqNo = req(r, "requestId", null);
        String job = opt(r, "job.type");
        return new LimitInquiry(reqNo, req(r, "applicant.ci", reqNo), req(r, "applicant.name", reqNo),
                digits8(req(r, "applicant.birthDate", reqNo), reqNo, "applicant.birthDate"),
                num(r, "income.annualIncome", reqNo, true),
                job == null ? "EMPLOYEE" : switch (job) {
                    case "FREELANCER" -> "FREELANCER";
                    case "SELF_EMPLOYED" -> "BUSINESS";
                    default -> "EMPLOYEE";
                },
                opt(r, "job.companyName"), opt(r, "collateral.vehicleNumber"), opt(r, "collateral.address"), null,
                num(r, "requestAmount", reqNo, false));
    }

    @Override
    public String response(LinkOutcome o) {
        ObjectNode n = om.createObjectNode();
        n.put("requestId", o.partnerReqNo());
        n.put("resultCode", o.outerCd());
        n.put("resultMessage", o.outerMsg());
        if (o.approved()) {
            ObjectNode offer = n.putObject("offer");
            offer.put("productCode", o.outerPrdCd());
            offer.put("productName", o.productNm());
            offer.put("limitAmount", o.limitAmt());
            offer.put("interestRate", BigDecimal.valueOf(o.rateBp()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        } else {
            n.putNull("offer");
        }
        return write(n);
    }

    @Override
    public String sampleRequest(SampleInput in) {
        ObjectNode n = om.createObjectNode();
        n.put("requestId", in.reqNo());
        ObjectNode a = n.putObject("applicant");
        a.put("ci", in.ci());
        a.put("name", in.name());
        a.put("birthDate", in.birth().substring(0, 4) + "-" + in.birth().substring(4, 6) + "-" + in.birth().substring(6));
        ObjectNode j = n.putObject("job");
        j.put("type", "BUSINESS".equals(in.employType()) ? "SELF_EMPLOYED" : in.employType());
        if (in.companyNm() != null) j.put("companyName", in.companyNm());
        n.putObject("income").put("annualIncome", in.annualIncome());
        if (in.vehicleNo() != null || in.collateralAddr() != null) {
            ObjectNode c = n.putObject("collateral");
            if (in.vehicleNo() != null) c.put("vehicleNumber", in.vehicleNo());
            if (in.collateralAddr() != null) c.put("address", in.collateralAddr());
        }
        if (in.requestAmt() > 0) n.put("requestAmount", in.requestAmt());
        return om.writerWithDefaultPrettyPrinter().writeValueAsString(n);
    }
}
