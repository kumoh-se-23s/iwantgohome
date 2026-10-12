package crowdfunding.protocol;

import java.io.Serializable;

/**
 * 클라이언트 → 서버 요청 메시지 = 무엇을(Command) + 데이터(payload).
 * ObjectOutputStream 으로 그대로 전송된다.
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Command command;
    private final Serializable payload;

    public Request(Command command) {
        this(command, null);
    }

    public Request(Command command, Serializable payload) {
        this.command = command;
        this.payload = payload;
    }

    public Command getCommand() { return command; }
    public Serializable getPayload() { return payload; }

    @Override
    public String toString() {
        return command + (payload == null ? "" : " (" + payload.getClass().getSimpleName() + ")");
    }
}
