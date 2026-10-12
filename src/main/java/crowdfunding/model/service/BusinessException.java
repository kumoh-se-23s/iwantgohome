package crowdfunding.model.service;

/**
 * 업무 규칙 위반 (예: 이미 후원한 프로젝트, 잔여 수량 없음, 권한 없음).
 * 서비스가 던지면 → 트랜잭션 롤백 → 컨트롤러가 Response.fail(메시지)로 바꿔 사용자에게 전달.
 */
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
