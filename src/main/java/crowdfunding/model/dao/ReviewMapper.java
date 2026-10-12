package crowdfunding.model.dao;

import crowdfunding.model.domain.Review;
import crowdfunding.model.dto.ReviewView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** review 테이블 DAO — SQL 은 resources/mapper/ReviewMapper.xml */
public interface ReviewMapper {

    int insertReview(Review review);

    boolean existsByUserAndProject(@Param("userId") int userId, @Param("projectId") int projectId);

    /** 프로젝트의 리뷰 (리워드명 포함), 최신순 */
    List<ReviewView> findViewsByProject(int projectId);
}
