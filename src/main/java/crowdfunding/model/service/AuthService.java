package crowdfunding.model.service;

import crowdfunding.config.TransactionManager;
import crowdfunding.model.dao.UserMapper;
import crowdfunding.model.domain.User;
import crowdfunding.model.dto.LoginUser;

/** 로그인 (2차: 회원가입도 여기에 추가) */
public class AuthService {

    public LoginUser login(String loginId, String password) {
        Validator.require(!Validator.isBlank(loginId) && password != null && !password.isEmpty(),
                "아이디와 비밀번호를 입력하세요.");

        return TransactionManager.execute(session -> {
            User user = session.getMapper(UserMapper.class).findByLoginId(loginId.trim());
            // 아이디가 없는 경우와 비밀번호가 틀린 경우를 구분하지 않는다 (계정 존재 여부 노출 방지)
            if (user == null || !user.getPassword().equals(password)) {
                throw new BusinessException("아이디 또는 비밀번호가 올바르지 않습니다.");
            }
            return new LoginUser(user.getUserId(), user.getLoginId(), user.getUserName(), user.getRole());
        });
    }

    // [2차] 회원가입 (서포터)
    // public int signUp(SignUpForm form) {
    //     검증 → userMapper.insertUser(role=SUPPORTER) → userMapper.insertSupporter(...) 를 한 트랜잭션으로
    //     아이디 중복은 UNIQUE(uk_user_login_id) → DbErrors.isDuplicateKey 로 "이미 사용 중인 아이디" 처리
    // }
}
