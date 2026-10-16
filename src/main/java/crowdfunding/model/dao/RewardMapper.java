package crowdfunding.model.dao;

import crowdfunding.model.domain.Reward;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** reward 테이블 SQL (resources/mapper/RewardMapper.xml) */
public interface RewardMapper {

    // ===== A: 리워드 등록 (검사 03) =====
    /** 리워드 등록. 생성된 PK 는 reward.rewardId 에 채워진다 */
    int insert(Reward reward);

    int countByProject(@Param("projectId") int projectId);

    /** 수량이 제한된 리워드 개수 */
    int countLimitedByProject(@Param("projectId") int projectId);

    List<Reward> findByProject(@Param("projectId") int projectId);

    // ===== C: 리워드 신청 · 동시성 (검사 08·10) =====
    Reward findById(@Param("rewardId") int rewardId);

    /** ★ SELECT ... FOR UPDATE : 이 리워드 행을 잠근다 (트랜잭션이 끝날 때까지 다른 신청은 대기) */
    Reward findByIdForUpdate(@Param("rewardId") int rewardId);

    /** 신청 수량 +1. 수량이 남아 있을 때만 바뀌고, 바뀐 행 수(0 또는 1)를 반환 */
    int increaseAppliedCount(@Param("rewardId") int rewardId, @Param("projectId") int projectId);
}
