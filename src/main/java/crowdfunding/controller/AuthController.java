package crowdfunding.controller;

import crowdfunding.model.dto.LoginForm;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.service.AuthService;
import crowdfunding.protocol.Response;

/** 로그인 / (2차) 회원가입 */
public class AuthController extends BaseController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public Response<LoginUser> login(LoginForm form) {
        return handle(() -> {
            if (form == null) {
                return Response.fail("아이디와 비밀번호를 입력하세요.");
            }
            LoginUser user = authService.login(form.getLoginId(), form.getPassword());
            return Response.ok(user.getUserName() + "님 환영합니다. (" + user.getRole().getLabel() + ")", user);
        });
    }

    // [2차] public Response<Integer> signUp(SignUpForm form) { ... }
}
