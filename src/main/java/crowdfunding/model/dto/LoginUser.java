package crowdfunding.model.dto;

import crowdfunding.model.enums.Role;

import java.io.Serializable;

/**
 * 로그인에 성공한 사용자 정보 (비밀번호는 담지 않음).
 * <ul>
 *   <li>1차: 테스트 코드가 로그인 결과로 받아 컨트롤러에 직접 넘긴다.</li>
 *   <li>2차: 서버의 ClientSession 이 접속마다 보관하고, 요청이 올 때마다 컨트롤러에 넘긴다.
 *       (클라이언트가 위조할 수 없도록 서버 쪽에서만 관리)</li>
 * </ul>
 */
public class LoginUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int userId;
    private final String loginId;
    private final String userName;
    private final Role role;

    public LoginUser(int userId, String loginId, String userName, Role role) {
        this.userId = userId;
        this.loginId = loginId;
        this.userName = userName;
        this.role = role;
    }

    public int getUserId() { return userId; }
    public String getLoginId() { return loginId; }
    public String getUserName() { return userName; }
    public Role getRole() { return role; }

    @Override
    public String toString() {
        return userName + "(" + loginId + ", " + role.getLabel() + ")";
    }
}
