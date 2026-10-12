package crowdfunding;

import crowdfunding.config.AppConfig;
import crowdfunding.view.ClientApp;

/**
 * ================== 2차: 클라이언트 실행 ==================
 * 메이커 / 서포터 노트북에서 실행 (DB 접속 정보 불필요 — 서버하고만 통신).
 *
 * 실행 인자: [서버IP] [포트]   예) java -cp crowdfunding.jar crowdfunding.ClientMain 192.168.0.10 9000
 */
public class ClientMain {

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : AppConfig.DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : AppConfig.SERVER_PORT;
        new ClientApp(host, port).run();
    }
}
