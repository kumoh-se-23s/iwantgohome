package crowdfunding.model.dto;

import java.io.Serializable;

/** 로그인 요청 (아이디, 비밀번호) */
public class LoginForm implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String loginId;
    private final String password;

    public LoginForm(String loginId, String password) {
        this.loginId = loginId;
        this.password = password;
    }

    public String getLoginId() { return loginId; }
    public String getPassword() { return password; }
}
