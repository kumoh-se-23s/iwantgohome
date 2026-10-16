package crowdfunding.model.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * user 테이블 한 행.
 *
 * DB 컬럼 id(로그인 아이디)는 user_id(PK)와 헷갈리기 쉬워서 자바에서는 loginId 로 부른다.
 * (Mapper XML 에서 "id AS login_id" 로 별칭을 붙여 연결)
 *
 * [담당] 공통 기반
 */
@Getter
@Setter
@ToString
public class User {

    private int userId;          // user_id      PK
    private String loginId;      // id           로그인 아이디 (5~10자)
    @ToString.Exclude   // 비밀번호는 출력하지 않는다
    private String pw;           // pw           비밀번호 (8~16자)
    private String role;         // role         supporter / maker / admin
    private String userName;     // user_name    이름 (2~10자)
    private String phoneNumber;  // phone_number 010-0000-0000
}
