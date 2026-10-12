package crowdfunding.model.service;

import java.sql.SQLException;

/**
 * MySQL 오류 코드를 사용자 메시지로 바꿔 주는 도우미.
 * 서비스에서 미리 검사하지만, 동시에 들어온 요청 등으로 DB 제약조건에 걸리는 경우의 마지막 안전망이다.
 */
public final class DbErrors {

    public static final int DUPLICATE_KEY = 1062;        // PK / UNIQUE 중복
    public static final int FK_PARENT_NOT_FOUND = 1452;  // 참조 대상 없음
    public static final int DATA_TOO_LONG = 1406;        // 컬럼 길이 초과
    public static final int LOCK_WAIT_TIMEOUT = 1205;    // 락 대기 시간 초과
    public static final int DEADLOCK = 1213;             // 교착 상태
    public static final int CHECK_VIOLATION = 3819;      // CHECK 제약 위반

    private DbErrors() {
    }

    /** 예외 원인 사슬에서 SQLException 의 오류 코드를 찾는다 (없으면 -1) */
    public static int errorCode(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof SQLException) {
                return ((SQLException) cur).getErrorCode();
            }
            cur = cur.getCause();
        }
        return -1;
    }

    public static boolean isDuplicateKey(Throwable e) {
        return errorCode(e) == DUPLICATE_KEY;
    }

    /** 알려진 DB 오류는 BusinessException(사용자 메시지)으로, 나머지는 그대로 돌려준다. */
    public static RuntimeException translate(RuntimeException e) {
        if (e instanceof BusinessException) {
            return e;
        }
        switch (errorCode(e)) {
            case DUPLICATE_KEY:
                return new BusinessException("이미 등록된 데이터입니다. (중복)", e);
            case FK_PARENT_NOT_FOUND:
                return new BusinessException("참조하는 데이터가 존재하지 않습니다.", e);
            case DATA_TOO_LONG:
                return new BusinessException("입력값이 허용 길이를 초과했습니다.", e);
            case CHECK_VIOLATION:
                return new BusinessException("DB 제약조건을 위반하는 값입니다: " + rootMessage(e), e);
            case LOCK_WAIT_TIMEOUT:
            case DEADLOCK:
                return new BusinessException("다른 요청과 충돌했습니다. 잠시 후 다시 시도해 주세요.", e);
            default:
                return e;
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur.getMessage();
    }
}
