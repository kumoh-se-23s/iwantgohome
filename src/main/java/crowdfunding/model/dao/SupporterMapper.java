package crowdfunding.model.dao;

import crowdfunding.model.domain.Supporter;
import org.apache.ibatis.annotations.Param;

/** supporter 테이블 SQL (resources/mapper/SupporterMapper.xml) */
public interface SupporterMapper {

    Supporter findByUserId(@Param("userId") int userId);

    /** 서포터 프로필이 있는 사용자인지 (후원은 서포터만 가능) */
    boolean exists(@Param("userId") int userId);

    /** 배송지 · 생년월일 수정 */
    int update(Supporter supporter);
}
