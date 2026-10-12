package crowdfunding.network.server;

import crowdfunding.config.AppConfig;
import crowdfunding.controller.RequestDispatcher;
import crowdfunding.model.service.FundingCloseService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * [2차] 멀티스레드 TCP 서버.
 * <ul>
 *   <li>접속이 들어올 때마다 ClientHandler 를 스레드 풀에서 실행 → 다수 사용자 동시 접속</li>
 *   <li>서버 시작 시 + 주기적으로 종료일이 지난 프로젝트를 성공/실패 처리</li>
 * </ul>
 */
public class FundingServer {

    private final int port;
    private final RequestDispatcher dispatcher;
    private final FundingCloseService closeService;
    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public FundingServer(int port, RequestDispatcher dispatcher, FundingCloseService closeService) {
        this.port = port;
        this.dispatcher = dispatcher;
        this.closeService = closeService;
    }

    public void start() throws IOException {
        closeExpired();   // 서버가 꺼져 있던 동안 끝난 프로젝트 처리
        scheduler.scheduleAtFixedRate(this::closeExpired,
                AppConfig.CLOSE_CHECK_INTERVAL_SECONDS, AppConfig.CLOSE_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[서버] 크라우드펀딩 서버 시작 — 포트 " + port + " (종료: Ctrl+C)");
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                clientPool.execute(new ClientHandler(socket, dispatcher));
            }
        } finally {
            scheduler.shutdownNow();
            clientPool.shutdownNow();
        }
    }

    private void closeExpired() {
        try {
            int closed = closeService.closeExpiredProjects();
            if (closed > 0) {
                System.out.println("[서버] 펀딩 기간이 끝난 프로젝트 " + closed + "건을 성공/실패 처리했습니다.");
            }
        } catch (RuntimeException e) {
            System.err.println("[서버] 펀딩 마감 처리 실패: " + e);
        }
    }
}
