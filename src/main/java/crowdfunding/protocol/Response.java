package crowdfunding.protocol;

import java.io.Serializable;

/**
 * 컨트롤러의 처리 결과 = 서버 → 클라이언트 응답 메시지.
 * <ul>
 *   <li>1차: 테스트 코드가 컨트롤러를 직접 호출해서 받는다.</li>
 *   <li>2차: 서버가 이 객체를 그대로 소켓으로 보낸다. (그래서 Serializable)</li>
 * </ul>
 *
 * @param <T> 결과 데이터 타입 (목록이면 ArrayList 등 직렬화 가능한 객체)
 */
public class Response<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String message;
    private final T data;

    private Response(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> Response<T> ok(String message, T data) {
        return new Response<>(true, message, data);
    }

    public static <T> Response<T> ok(T data) {
        return new Response<>(true, "성공", data);
    }

    public static <T> Response<T> fail(String message) {
        return new Response<>(false, message, null);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }

    @Override
    public String toString() {
        return (success ? "[성공] " : "[실패] ") + message;
    }
}
