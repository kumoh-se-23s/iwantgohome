package crowdfunding.model.domain;

import crowdfunding.model.enums.Role;

import java.io.Serializable;

/**
 * user 테이블 1행.
 * DB 컬럼 id / pw 는 의미가 잘 드러나도록 자바에서는 loginId / password 로 부른다
 * (UserMapper.xml 의 resultMap 에서 매핑).
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer userId;       // user_id (PK, AUTO_INCREMENT)
    private String loginId;       // id      (로그인 아이디 5~10자, 대소문자 구분)
    private String password;      // pw      (8~16자)
    private Role role;            // role
    private String userName;      // user_name (2~10자)
    private String phoneNumber;   // phone_number (010-0000-0000)

    public User() {
    }

    public User(String loginId, String password, Role role, String userName, String phoneNumber) {
        this.loginId = loginId;
        this.password = password;
        this.role = role;
        this.userName = userName;
        this.phoneNumber = phoneNumber;
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
