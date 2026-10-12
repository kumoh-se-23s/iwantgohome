package crowdfunding.network.server;

import crowdfunding.controller.RequestDispatcher;
import crowdfunding.protocol.Request;
import crowdfunding.protocol.Response;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * [2차] 클라이언트 한 명을 담당하는 스레드.
 * Request 를 받아 → RequestDispatcher(→ 컨트롤러 → 서비스 → DB) → Response 를 돌려준다.
 * 여러 클라이언트가 동시에 접속하면 ClientHandler 가 여러 개 동시에 실행된다.
 * 동시에 같은 리워드를 신청해도 SupporterService.applyReward 의 DB 락이 순서를 보장한다.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final RequestDispatcher dispatcher;
    private final ClientSession session;

    public ClientHandler(Socket socket, RequestDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
        this.session = new ClientSession(socket.getRemoteSocketAddress().toString());
    }

    @Override
    public void run() {
        log("접속");
        try (Socket s = socket;
             // 주의: 양쪽 모두 ObjectOutputStream 을 먼저 만들고 flush 해야 서로 헤더를 기다리며 멈추지 않는다
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
            out.flush();
            ObjectInputStream in = new ObjectInputStream(s.getInputStream());

            while (true) {
                Object received = in.readObject();          // 클라이언트가 연결을 끊으면 EOFException
                Response<?> response;
                if (received instanceof Request) {
                    Request request = (Request) received;
                    String who = session.toString();             // 로그아웃 요청이어도 누가 보냈는지 남기기 위해 먼저 기록
                    response = dispatcher.dispatch(request, session);
                    log(who, request.getCommand() + " → " + (response.isSuccess() ? "성공" : "실패: " + response.getMessage()));
                } else {
                    response = Response.fail("알 수 없는 메시지입니다.");
                }
                out.writeObject(response);
                out.flush();
                out.reset();   // 같은 객체를 다시 보낼 때 예전 내용이 재사용되지 않도록 스트림 캐시 초기화
            }
        } catch (EOFException | SocketException e) {
            log("접속 종료");
        } catch (IOException | ClassNotFoundException e) {
            log("통신 오류: " + e);
        }
    }

    private void log(String message) {
        log(session.toString(), message);
    }

    private void log(String who, String message) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + who + " - " + message);
    }
}
