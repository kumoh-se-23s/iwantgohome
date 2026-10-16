package crowdfunding.model.dao;

import crowdfunding.model.domain.Funding;
import org.apache.ibatis.annotations.Param;

/** funding 테이블 SQL (resources/mapper/FundingMapper.xml) */
public interface FundingMapper {

    /** 이 서포터가 이 프로젝트에 이미 후원했는지 */
    boolean exists(@Param("userId") int userId, @Param("projectId") int projectId);

    /** 후원 기록 추가 (신청일은 DB 기본값 CURRENT_TIMESTAMP) */
    int insert(@Param("userId") int userId, @Param("projectId") int projectId, @Param("rewardId") int rewardId);

    Funding find(@Param("userId") int userId, @Param("projectId") int projectId);

    /** 이 리워드의 실제 후원 기록 수 (applied_count 와 같아야 정상) */
    int countByReward(@Param("rewardId") int rewardId);
}
