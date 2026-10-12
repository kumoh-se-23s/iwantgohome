package crowdfunding.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 리워드별 펀딩 현황 = 리워드 정보(신청 수량/잔여 수량) + 신청한 서포터 목록.
 * RewardMapper.xml 의 resultMap(&lt;collection&gt;)이 조인 결과를 이 구조로 묶어 준다.
 */
public class RewardFundingStatus extends RewardView {
    private static final long serialVersionUID = 1L;

    private List<FundingSupporter> supporters = new ArrayList<>();

    public List<FundingSupporter> getSupporters() { return supporters; }
    public void setSupporters(List<FundingSupporter> supporters) { this.supporters = supporters; }
}
