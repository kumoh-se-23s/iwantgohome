package crowdfunding.config;

import crowdfunding.model.service.DbErrors;
import org.apache.ibatis.session.SqlSession;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 서비스 메소드 하나 = 트랜잭션 하나.
 *
 * <pre>
 *   TransactionManager.execute(session -> {
 *       ProjectMapper mapper = session.getMapper(ProjectMapper.class);
 *       ... DB 작업 ...
 *       return 결과;
 *   });
 * </pre>
 * <ul>
 *   <li>작업이 정상적으로 끝나면 commit</li>
 *   <li>도중에 예외(BusinessException 포함)가 나면 rollback → 앞에서 한 INSERT/UPDATE 도 모두 취소</li>
 *   <li>SqlSession 은 매번 새로 열고 닫는다 → 여러 스레드(2차 서버의 접속자들)가 동시에 호출해도 안전</li>
 * </ul>
 */
public final class TransactionManager {

    private TransactionManager() {
    }

    /** 결과값이 있는 작업 */
    public static <T> T execute(Function<SqlSession, T> work) {
        // openSession(false) : 자동 커밋 끔 → 직접 commit / rollback
        try (SqlSession session = MyBatisConfig.getSqlSessionFactory().openSession(false)) {
            try {
                T result = work.apply(session);
                session.commit();
                return result;
            } catch (RuntimeException e) {
                session.rollback(true);       // true: 변경 여부와 상관없이 강제 롤백 (FOR UPDATE 락도 즉시 해제)
                throw DbErrors.translate(e);  // DB 오류 코드 → 사용자에게 보여줄 메시지
            }
        }
    }

    /** 결과값이 없는 작업 */
    public static void run(Consumer<SqlSession> work) {
        execute(session -> {
            work.accept(session);
            return null;
        });
    }
}
