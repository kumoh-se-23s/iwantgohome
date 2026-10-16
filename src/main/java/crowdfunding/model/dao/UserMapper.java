package crowdfunding.model.dao;

import crowdfunding.model.domain.User;
import org.apache.ibatis.annotations.Param;

/**
 * user 테이블 SQL. 실제 SQL 은 resources/mapper/UserMapper.xml 에 있다.
 * (인터페이스 메소드 이름 = XML 의 id)
 */
public interface UserMapper {

    User findById(@Param("userId") int userId);

    /** 이름 · 연락처 · 비밀번호 수정 */
    int updateProfile(User user);
}
