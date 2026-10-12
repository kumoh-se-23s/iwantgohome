package crowdfunding.network.client;

import crowdfunding.protocol.Command;
import crowdfunding.protocol.Request;
import crowdfunding.protocol.Response;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

/**
 * [2차] 클라이언트 쪽 서버 연결. View(메뉴)는 이 클래스의 send() 만 호출한다.
 * 요청 하나 보내고 응답 하나 받는 동기 방식.
 */
public class ServerConnection implements Closeable {

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private volatile boolean broken;   // 통신 오류가 한 번이라도 나면 true → 클라이언트 종료

    public ServerConnection(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());   // 서버와 마찬가지로 출력 스트림 먼저
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }

    public <T> Response<T> send(Command command) {
        return send(command, null);
    }

    /** 요청을 보내고 응답을 기다린다. 통신이 끊기면 실패 응답을 돌려준다. */
    @SuppressWarnings("unchecked")
    public synchronized <T> Response<T> send(Command command, Serializable payload) {
        try {
            out.writeObject(new Request(command, payload));
            out.flush();
            out.reset();
            return (Response<T>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            broken = true;
            return Response.fail("서버와의 연결이 끊어졌습니다. (" + e.getMessage() + ")");
        }
    }

    public boolean isConnected() {
        return !broken && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
