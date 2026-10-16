package crowdfunding.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * [검사 1] 펀딩 현황 안의 리워드 한 개: 신청 수량 / 잔여 수량 / 신청한 서포터 목록
 *
 * [담당] D (검사 01)
 */
@Getter
@Setter
@ToString
public class RewardStatus {

    private int rewardId;
    private String rewardName;
    private int rewardPrice;
    private Integer rewardAmount;    // 제공 가능 수량 (NULL = 무제한)
    private int appliedCount;        // 신청 수량
    private List<FunderInfo> funders = new ArrayList<>();

    /** 잔여 수량 (무제한이면 null) */
    public Integer getRemaining() {
        return rewardAmount == null ? null : rewardAmount - appliedCount;
    }
}
