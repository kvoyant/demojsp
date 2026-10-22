package com.example.demojsp.common.platformlink.adapter;

import com.example.demojsp.common.platformlink.model.CssProduct;
import com.example.demojsp.common.platformlink.model.LimitInquiry;
import com.example.demojsp.common.platformlink.model.LinkOutcome;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 핀다형 전문 (FINDA_V1) - 모기지 전용 제휴. ※ 연동규격 수령 전 <b>가정</b> 형식.
 * <pre>
 * 요청 {"applicationNo":"FD-001","userCi":"..","userName":"홍길동","userBirth":"19900101","annualIncomeWon":60000000,
 *       "jobType":"WORKER|FREELANCE|OWNER","property":{"address":"서울 ..","type":"APT"},"hopeAmount":200000000}
 * 응답 {"applicationNo":"FD-001","status":"OK","reason":"승인","loan":{"code":"FD-HOME-PEPPER","name":"..","maxAmount":..,"minRate":"5.90"}}
 * </pre>
 * 특징: 담보(property.address)가 필수, 금리를 문자열 "5.90" 으로 받는다.
 */
@Component
public class FindaAdapter extends JsonPartnerAdapter {

    public FindaAdapter(ObjectMapper om) {
        super(om);
    }

    @Override
    public String adapterCd() {
        return "FINDA_V1";
    }

    @Override
    public String description() {
        return "flat camelCase · 모기지 전용(property.address 필수) · 금액 원 · 금리 문자열 \"5.90\"";
    }

    @Override
    public LimitInquiry parse(String body) {
        JsonNode r = tree(body, "applicationNo");
        String reqNo = req(r, "applicationNo", null);
        String job = opt(r, "jobType");
        return new LimitInquiry(reqNo, req(r, "userCi", reqNo), req(r, "userName", reqNo),
                digits8(req(r, "userBirth", reqNo), reqNo, "userBirth"), num(r, "annualIncomeWon", reqNo, true),
                job == null ? "EMPLOYEE" : switch (job) {
                    case "FREELANCE" -> "FREELANCER";
                    case "OWNER" -> "BUSINESS";
                    default -> "EMPLOYEE";
                },
                null, null, req(r, "property.address", reqNo), CssProduct.MORTGAGE, num(r, "hopeAmount", reqNo, false));
    }

    @Override
    public String response(LinkOutcome o) {
        ObjectNode n = om.createObjectNode();
        n.put("applicationNo", o.partnerReqNo());
        n.put("status", o.outerCd());
        n.put("reason", o.outerMsg());
        if (o.approved()) {
            ObjectNode loan = n.putObject("loan");
            loan.put("code", o.outerPrdCd());
            loan.put("name", o.productNm());
            loan.put("maxAmount", o.limitAmt());
            loan.put("minRate", BigDecimal.valueOf(o.rateBp()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString());
        }
        return write(n);
    }

    @Override
    public String sampleRequest(SampleInput in) {
        ObjectNode n = om.createObjectNode();
        n.put("applicationNo", in.reqNo());
        n.put("userCi", in.ci());
        n.put("userName", in.name());
        n.put("userBirth", in.birth());
        n.put("annualIncomeWon", in.annualIncome());
        n.put("jobType", switch (in.employType()) {
            case "FREELANCER" -> "FREELANCE";
            case "BUSINESS" -> "OWNER";
            default -> "WORKER";
        });
        ObjectNode p = n.putObject("property");
        p.put("address", in.collateralAddr() == null ? "서울특별시 강남구 테헤란로 123 페퍼아파트 101동 1001호" : in.collateralAddr());
        p.put("type", "APT");
        if (in.requestAmt() > 0) n.put("hopeAmount", in.requestAmt());
        return om.writerWithDefaultPrettyPrinter().writeValueAsString(n);
    }
}
