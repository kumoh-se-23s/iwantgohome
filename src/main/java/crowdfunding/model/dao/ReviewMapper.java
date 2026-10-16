package crowdfunding.model.dao;

import crowdfunding.model.domain.Review;
import crowdfunding.model.dto.ReviewView;
import org.apache.ibatis.annotations.Param;

/** review 테이블 SQL (resources/mapper/ReviewMapper.xml) */
public interface ReviewMapper {

    boolean exists(@Param("userId") int userId, @Param("projectId") int projectId);

    /** 리뷰 등록. 생성된 PK 는 review.reviewId 에 채워진다 */
    int insert(Review review);

    /** 리워드명 / 별점 / 리뷰 출력용 */
    ReviewView findView(@Param("userId") int userId, @Param("projectId") int projectId);
}
