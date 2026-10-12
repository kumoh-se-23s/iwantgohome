package crowdfunding.model.dao;

import crowdfunding.model.domain.Supporter;
import crowdfunding.model.domain.User;
import crowdfunding.model.dto.SupporterProfile;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/** user / supporter 테이블 DAO — SQL 은 resources/mapper/UserMapper.xml */
public interface UserMapper {

    User findByLoginId(String loginId);

    User findById(int userId);

    SupporterProfile findSupporterProfile(int userId);

    /** INSERT 후 생성된 user_id 가 user.userId 에 채워진다 */
    int insertUser(User user);

    int insertSupporter(Supporter supporter);

    /** password 가 null 이면 비밀번호는 바꾸지 않는다 */
    int updateUserInfo(@Param("userId") int userId,
                       @Param("userName") String userName,
                       @Param("phoneNumber") String phoneNumber,
                       @Param("password") String password);

    int updateSupporterInfo(@Param("userId") int userId,
                            @Param("deliveryAddress") String deliveryAddress,
                            @Param("birthDate") LocalDate birthDate);
}
