package crowdfunding;

import crowdfunding.config.AppConfig;
import crowdfunding.config.AppContext;
import crowdfunding.network.server.FundingServer;

import java.io.IOException;

/**
 * ================== 2차: 서버 실행 ==================
 * 서버 노트북에서 실행 (MySQL 도 이 노트북에 있음).
 * 관리자는 같은 노트북에서 ClientMain 을 하나 더 실행해 admin01 로 로그인한다.
 *
 * 실행 인자: [포트]   (기본 9000)
 */
public class ServerMain {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : AppConfig.SERVER_PORT;
        AppContext ctx = AppContext.getInstance();   // 1차 검사와 같은 컨트롤러/서비스 구성
        new FundingServer(port, ctx.dispatcher(), ctx.fundingCloseService()).start();
    }
}
