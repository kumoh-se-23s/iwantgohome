package crowdfunding.config;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;

/**
 * 커넥션 풀(DataSource)을 받아서 MyBatis 의 SqlSessionFactory 를 만든다.
 *
 *   PooledDataSource (DBCP 커넥션 풀)  ─→  MyBatisConfig  ─→  SqlSessionFactory  ─→  각 Service
 *
 * - mybatis-config.xml : 설정(카멜케이스 자동 변환 등), 별칭, Mapper XML 목록
 * - DataSource         : MyBatis 기본 풀 대신 3주차에 배운 DBCP 커넥션 풀을 그대로 사용
 * - 트랜잭션            : JDBC 방식 → Service 가 commit() / rollback() 을 직접 호출
 *
 * Main 에서 한 번만 만들고, 각 Service 생성자에 넘겨준다.
 * SqlSessionFactory 는 여러 스레드가 같이 써도 안전하다.
 *
 * [담당] 공통 기반
 */
public final class MyBatisConfig {

    private static final String MYBATIS_CONFIG = "mybatis-config.xml";

    private MyBatisConfig() {
    }

    public static SqlSessionFactory createSqlSessionFactory(DataSource dataSource) {
        try (Reader reader = Resources.getResourceAsReader(MYBATIS_CONFIG)) {
            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader);
            // mybatis-config.xml 에는 DB 접속 정보를 두지 않고, 여기서 DBCP 커넥션 풀을 연결한다
            factory.getConfiguration().setEnvironment(
                    new Environment("local", new JdbcTransactionFactory(), dataSource));
            return factory;
        } catch (IOException e) {
            throw new UncheckedIOException(MYBATIS_CONFIG + " 파일을 읽을 수 없습니다.", e);
        }
    }
}
