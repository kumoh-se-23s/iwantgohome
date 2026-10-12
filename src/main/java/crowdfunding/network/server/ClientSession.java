package crowdfunding.network.server;

import crowdfunding.model.dto.LoginUser;

/**
 * [2차] 접속(소켓) 하나당 하나씩 서버가 가지고 있는 세션 정보.
 * 로그인에 성공하면 LoginUser 를 저장해 두고, 이후 요청마다 컨트롤러에 넘긴다.
 * ClientHandler 스레드 하나만 사용하므로 동기화가 필요 없다.
 */
public class ClientSession {

    private final String clientAddress;
    private LoginUser loginUser;

    public ClientSession(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public void login(LoginUser user) {
        this.loginUser = user;
    }

    public void logout() {
        this.loginUser = null;
    }

    public LoginUser getLoginUser() {
        return loginUser;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    @Override
    public String toString() {
        return clientAddress + (loginUser == null ? " (로그인 전)" : " " + loginUser);
    }
}
