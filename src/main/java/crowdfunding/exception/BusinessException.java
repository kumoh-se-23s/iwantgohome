package crowdfunding.exception;

/**
 * 업무 규칙 위반을 알리는 예외.
 * 예) "이미 후원한 프로젝트입니다", "리워드 잔여 수량이 부족합니다"
 *
 * - Service 는 규칙에 어긋나면 이 예외를 던진다 → 트랜잭션은 롤백된다.
 * - 테스트(나중에는 Controller)는 이 예외를 잡아서 메시지만 사용자에게 보여 준다.
 * - RuntimeException 이라서 메소드마다 throws 를 붙이지 않아도 된다 (3주차: 언체크 예외).
 *
 * [담당] 공통 기반
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }
}
