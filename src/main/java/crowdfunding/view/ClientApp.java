package crowdfunding.view;

import crowdfunding.model.dto.LoginForm;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.network.client.ServerConnection;
import crowdfunding.protocol.Command;
import crowdfunding.protocol.Response;

import java.io.IOException;

/**
 * [2차] 클라이언트 프로그램의 시작 화면.
 * 로그인 → 사용자 유형(메이커/관리자/서포터)에 맞는 메뉴 → 로그아웃 → 다시 로그인 화면.
 * 프로그램을 재시작하지 않고 여러 계정으로 계속 시연할 수 있다.
 */
public class ClientApp {

    private final String host;
    private final int port;
    private final ConsoleInput in = new ConsoleInput();

    public ClientApp(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() {
        try (ServerConnection conn = new ServerConnection(host, port)) {
            System.out.println("크라우드펀딩 서버(" + host + ":" + port + ")에 접속했습니다.");
            while (conn.isConnected()) {
                System.out.println();
                System.out.println("========== 크라우드펀딩 ==========");
                System.out.println("  1. 로그인");
                // [2차 추가] System.out.println("  2. 회원가입 (서포터)");
                System.out.println("  0. 종료");
                int choice = in.number("선택> ");
                if (choice == 0) {
                    break;
                }
                if (choice == 1) {
                    login(conn);
                }
            }
            System.out.println("프로그램을 종료합니다.");
        } catch (IOException e) {
            System.out.println("서버(" + host + ":" + port + ")에 접속할 수 없습니다. 서버가 실행 중인지 확인하세요. (" + e.getMessage() + ")");
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage() + " 프로그램을 종료합니다.");   // 입력 스트림 종료(Ctrl+D 등)
        }
    }

    private void login(ServerConnection conn) {
        String loginId = in.text("아이디: ");
        String password = in.text("비밀번호: ");
        Response<LoginUser> res = conn.send(Command.LOGIN, new LoginForm(loginId, password));
        ResultView.printResponse(res);
        if (!res.isSuccess()) {
            return;
        }
        LoginUser user = res.getData();
        switch (user.getRole()) {
            case MAKER:     new MakerMenu(conn, in, user).run(); break;
            case ADMIN:     new AdminMenu(conn, in, user).run(); break;
            case SUPPORTER: new SupporterMenu(conn, in, user).run(); break;
            default: break;
        }
    }
}
