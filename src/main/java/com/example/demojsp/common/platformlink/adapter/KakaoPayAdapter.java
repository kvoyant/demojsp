package com.example.demojsp.common.platformlink.adapter;

import com.example.demojsp.common.platformlink.model.CssProduct;
import com.example.demojsp.common.platformlink.model.LimitInquiry;
import com.example.demojsp.common.platformlink.model.LinkOutcome;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 카카오페이형 전문 (KAKAOPAY_V1) - ※ 연동규격 수령 전 <b>가정</b> 형식.
 * <pre>
 * 요청 {"tx_id":"KP-001","user":{"ci":"..","name":"홍길동","birth_ymd":"19900101"},"job_cd":"01","company_nm":"..",
 *       "income_manwon":5000,"prd_type":"C|A|M","car_no":"12가3456","house_addr":"..","req_amt_manwon":1000}
 * 응답 {"tx_id":"KP-001","res_cd":"0000","res_msg":"정상","offer":{"prd_cd":"KP0201","prd_nm":"..","lmt_amt_manwon":3000,"rate_bp":990}}
 * </pre>
 * 특징: snake_case, <b>금액이 만원 단위</b>, 직업이 숫자코드(01 직장인 / 02 프리랜서 / 03 사업자), 상품유형을 직접 지정.
 * → 어댑터에서 원 단위·당행 코드로 바꿔 공통모델에는 차이가 남지 않게 한다.
 */
@Component
public class KakaoPayAdapter extends JsonPartnerAdapter {

    public KakaoPayAdapter(ObjectMapper om) {
        super(om);
    }

    @Override
    public String adapterCd() {
        return "KAKAOPAY_V1";
    }

    @Override
    public String description() {
        return "snake_case · 금액 만원 단위 · 직업 숫자코드(01/02/03) · 상품유형 직접 지정(prd_type C/A/M) · 금리 bp";
    }

    @Override
    public LimitInquiry parse(String body) {
        JsonNode r = tree(body, "tx_id");
        String reqNo = req(r, "tx_id", null);
        String jobCd = opt(r, "job_cd");
        String prd = opt(r, "prd_type");
        String hint = prd == null ? null : switch (prd) {
            case "C" -> CssProduct.CREDIT;
            case "A" -> CssProduct.AUTO;
            case "M" -> CssProduct.MORTGAGE;
            default -> throw new AdapterParseException("prd_type 값 오류: " + prd, reqNo);
        };
        return new LimitInquiry(reqNo, req(r, "user.ci", reqNo), req(r, "user.name", reqNo),
                digits8(req(r, "user.birth_ymd", reqNo), reqNo, "user.birth_ymd"),
                num(r, "income_manwon", reqNo, true) * 10_000,                       // 만원 → 원
                jobCd == null ? "EMPLOYEE" : switch (jobCd) {
                    case "02" -> "FREELANCER";
                    case "03" -> "BUSINESS";
                    default -> "EMPLOYEE";
                },
                opt(r, "company_nm"), opt(r, "car_no"), opt(r, "house_addr"), hint,
                num(r, "req_amt_manwon", reqNo, false) * 10_000);
    }

    @Override
    public String response(LinkOutcome o) {
        ObjectNode n = om.createObjectNode();
        n.put("tx_id", o.partnerReqNo());
        n.put("res_cd", o.outerCd());
        n.put("res_msg", o.outerMsg());
        if (o.approved()) {
            ObjectNode offer = n.putObject("offer");
            offer.put("prd_cd", o.outerPrdCd());
            offer.put("prd_nm", o.productNm());
            offer.put("lmt_amt_manwon", o.limitAmt() / 10_000);                       // 원 → 만원
            offer.put("rate_bp", o.rateBp());
        }
        return write(n);
    }

    @Override
    public String sampleRequest(SampleInput in) {
        ObjectNode n = om.createObjectNode();
        n.put("tx_id", in.reqNo());
        ObjectNode u = n.putObject("user");
        u.put("ci", in.ci());
        u.put("name", in.name());
        u.put("birth_ymd", in.birth());
        n.put("job_cd", switch (in.employType()) {
            case "FREELANCER" -> "02";
            case "BUSINESS" -> "03";
            default -> "01";
        });
        if (in.companyNm() != null) n.put("company_nm", in.companyNm());
        n.put("income_manwon", in.annualIncome() / 10_000);
        if (in.product() != null) n.put("prd_type", in.product().substring(0, 1));
        if (in.vehicleNo() != null) n.put("car_no", in.vehicleNo());
        if (in.collateralAddr() != null) n.put("house_addr", in.collateralAddr());
        if (in.requestAmt() > 0) n.put("req_amt_manwon", in.requestAmt() / 10_000);
        return om.writerWithDefaultPrettyPrinter().writeValueAsString(n);
    }
}
