package crowdfunding.exception;

import java.sql.SQLException;

/**
 * MyBatis 가 던진 예외 속에 들어 있는 MySQL 오류를 알아보기 위한 도우미.
 *
 * MyBatis 는 SQL 오류를 PersistenceException 으로 한 번 감싸서 던진다.
 * 그 안쪽(cause)을 따라가면 실제 SQLException 과 MySQL 오류 번호가 들어 있다.
 *
 * [담당] 공통 기반
 */
public final class DbErrors {

    /** MySQL 오류 번호 1062: PRIMARY KEY 또는 UNIQUE 값 중복 */
    private static final int ER_DUP_ENTRY = 1062;

    private DbErrors() {
    }

    /** 중복 키 오류(같은 서포터가 같은 프로젝트에 두 번 후원 등)인지 확인 */
    public static boolean isDuplicateKey(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && sql.getErrorCode() == ER_DUP_ENTRY) {
                return true;
            }
        }
        return false;
    }
}
