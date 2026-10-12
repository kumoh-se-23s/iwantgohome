package crowdfunding.model.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 메이커의 "프로젝트별 펀딩 현황" (1차 검사 1번 / 2차 4번)
 * <ul>
 *   <li>project : 총 펀딩 금액, 달성률, 후원자 수 등</li>
 *   <li>rewards : 리워드별 신청 수량 / 잔여 수량 / 서포터 및 배송지</li>
 * </ul>
 */
public class FundingStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ProjectSummary project;
    private final List<RewardFundingStatus> rewards;

    public FundingStatus(ProjectSummary project, List<RewardFundingStatus> rewards) {
        this.project = project;
        this.rewards = rewards;
    }

    public ProjectSummary getProject() { return project; }
    public List<RewardFundingStatus> getRewards() { return rewards; }
}
