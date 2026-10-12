package crowdfunding.model.dao;

import org.apache.ibatis.annotations.Param;

/** project_like 테이블 DAO — SQL 은 resources/mapper/LikeMapper.xml */
public interface LikeMapper {

    int insertLike(@Param("projectId") int projectId, @Param("userId") int userId);

    boolean existsLike(@Param("projectId") int projectId, @Param("userId") int userId);

    // [2차] 좋아요 취소(토글)용 deleteLike(projectId, userId) 를 여기에 추가
}
