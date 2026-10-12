package crowdfunding.model.dao;

import crowdfunding.model.domain.Funding;
import org.apache.ibatis.annotations.Param;

/** funding(신청 현황) 테이블 DAO — SQL 은 resources/mapper/FundingMapper.xml */
public interface FundingMapper {

    /** PK(user_id, project_id) 가 이미 있으면 DuplicateKey 오류 → 한 프로젝트에 한 번만 후원 */
    int insertFunding(Funding funding);

    Funding findByUserAndProject(@Param("userId") int userId, @Param("projectId") int projectId);
}
