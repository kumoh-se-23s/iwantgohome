package crowdfunding.config;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

/**
 * MyBatis SqlSessionFactory 를 프로그램 전체에서 하나만 만들어 공유한다. (스레드 안전)
 * <p>
 * SqlSession(=DB 커넥션 1개 + 트랜잭션)은 스레드 안전하지 않으므로
 * 요청(서비스 메소드 호출)마다 새로 열고 닫는다 → {@link TransactionManager}
 * <p>
 * 클라이언트 프로그램(ClientMain)은 이 클래스를 사용하지 않는다 — 사용자는 DB 에 직접 접근할 수 없음.
 */
public final class MyBatisConfig {

    private static final String CONFIG_FILE = "mybatis-config.xml";
    private static final String DB_PROPERTIES = "db.properties";
    private static volatile SqlSessionFactory factory;

    private MyBatisConfig() {
    }

    public static SqlSessionFactory getSqlSessionFactory() {
        if (factory == null) {
            synchronized (MyBatisConfig.class) {
                if (factory == null) {
                    factory = build();
                }
            }
        }
        return factory;
    }

    private static SqlSessionFactory build() {
        try (Reader reader = Resources.getResourceAsReader(CONFIG_FILE)) {
            return new SqlSessionFactoryBuilder().build(reader, loadDbProperties());
        } catch (IOException e) {
            throw new IllegalStateException(CONFIG_FILE + " 을 읽을 수 없습니다.", e);
        }
    }

    /**
     * db.properties 를 읽고, JVM 옵션(-Ddb.url=... -Ddb.username=... -Ddb.password=...)이 있으면 그 값으로 덮어쓴다.
     * → 노트북마다 비밀번호가 달라도 파일을 고치지 않고 실행 옵션으로 바꿀 수 있다.
     */
    private static Properties loadDbProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream in = Resources.getResourceAsStream(DB_PROPERTIES)) {
            props.load(in);
        }
        for (String key : new String[]{"db.driver", "db.url", "db.username", "db.password"}) {
            String override = System.getProperty(key);
            if (override != null) {
                props.setProperty(key, override);
            }
        }
        return props;
    }
}
