package crowdfunding.model.dao;

import crowdfunding.model.domain.Reward;
import crowdfunding.model.dto.RewardFundingStatus;
import crowdfunding.model.dto.RewardView;

import java.util.List;

/** reward 테이블 DAO — SQL 은 resources/mapper/RewardMapper.xml */
public interface RewardMapper {

    int insertReward(Reward reward);

    List<RewardView> findViewsByProject(int projectId);

    int countByProject(int projectId);

    /**
     * ★ 동시성 제어: SELECT ... FOR UPDATE 로 리워드 행에 배타 락을 건다.
     * 같은 리워드를 신청하는 다른 트랜잭션은 이 트랜잭션이 commit/rollback 할 때까지 대기한다.
     */
    Reward findByIdForUpdate(int rewardId);

    /** 신청 수량 +1 (수량 제한이 있고 이미 다 찼으면 0행 변경) */
    int increaseAppliedCount(int rewardId);

    /** 리워드별 신청 현황 + 신청한 서포터(배송지) 목록 */
    List<RewardFundingStatus> findFundingStatusByProject(int projectId);
}
