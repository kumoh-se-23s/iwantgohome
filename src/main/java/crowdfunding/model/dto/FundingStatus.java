package crowdfunding.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * [검사 1] 메이커의 "프로젝트별 펀딩 현황" 한 건.
 *
 *   프로젝트 (FundingStatus)
 *     └ 리워드 목록 (RewardStatus)      : 신청 수량 / 잔여 수량
 *         └ 서포터 목록 (FunderInfo)     : 이름, 연락처, 배송지
 *
 * ProjectMapper.xml 의 fundingStatusMap(3단 중첩 resultMap)이 쿼리 한 번으로 이 모양을 채운다.
 * 총 펀딩 금액은 실제 후원 기록(서포터 목록) × 리워드 가격으로 계산한다.
 *
 * [담당] D (검사 01)
 */
@Getter
@Setter
@ToString
public class FundingStatus {

    private int projectId;
    private String projectName;
    private String projectStatus;     // in_progress / success / fail
    private LocalDate startDate;
    private LocalDate endDate;
    private int targetAmount;
    private List<RewardStatus> rewards = new ArrayList<>();

    /** 프로젝트 총 펀딩 금액 = Σ (리워드 가격 × 그 리워드를 신청한 서포터 수) */
    public long getTotalAmount() {
        long total = 0;
        for (RewardStatus r : rewards) {
            total += (long) r.getRewardPrice() * r.getFunders().size();
        }
        return total;
    }

    /** 후원한 서포터 수 (프로젝트당 1회 후원이므로 후원 건수와 같다) */
    public int getSupporterCount() {
        int count = 0;
        for (RewardStatus r : rewards) {
            count += r.getFunders().size();
        }
        return count;
    }

    /** 달성률 (%) */
    public double getAchieveRate() {
        return targetAmount == 0 ? 0 : getTotalAmount() * 100.0 / targetAmount;
    }
}
