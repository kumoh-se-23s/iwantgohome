package crowdfunding.config;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

/**
 * 커넥션 풀(DBCP)을 프로그램 전체에서 하나만 만들어 두고 나눠 쓰는 클래스 (3주차 강의 PooledDataSource 와 같은 구조)
 *
 * - static 블록은 이 클래스가 처음 사용될 때 딱 한 번 실행된다 → 커넥션 풀이 하나만 만들어진다.
 * - 접속 정보는 src/main/resources/config/db.properties 에서 읽는다.
 * - FileInputStream 대신 getResourceAsStream: 클래스패스 기준으로 찾으므로 JAR 로 묶어도 동작한다 (3주차 강의).
 *
 * 이 DataSource 는 Main 에서 MyBatis(SqlSessionFactory)에 넘겨서 사용한다.
 *
 * [담당] 공통 기반
 */
public class PooledDataSource {

    private static final String DB_PROPERTIES = "config/db.properties";
    private static final List<String> KEYS = List.of("DRIVER_CLASS", "DB_CONNECTION_URL", "DB_USER", "DB_PWD");

    private static final BasicDataSource basicDS = new BasicDataSource();

    static {
        Properties properties = loadProperties();
        basicDS.setDriverClassName(properties.getProperty("DRIVER_CLASS")); // JDBC 드라이버 로드
        basicDS.setUrl(properties.getProperty("DB_CONNECTION_URL"));
        basicDS.setUsername(properties.getProperty("DB_USER"));
        basicDS.setPassword(properties.getProperty("DB_PWD"));
        // 커넥션 풀 설정
        basicDS.setInitialSize(10);   // 처음 getConnection() 할 때 미리 만들어 둘 커넥션 수
        basicDS.setMaxTotal(10);      // 동시에 빌려줄 수 있는 최대 커넥션 수
    }

    private PooledDataSource() {
    }

    public static DataSource getDataSource() {
        return basicDS;
    }

    /** db.properties 를 읽고, 실행 옵션(-DDB_PWD=...)이 있으면 그 값을 우선 사용한다. */
    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream in = PooledDataSource.class.getClassLoader().getResourceAsStream(DB_PROPERTIES)) {
            if (in == null) {
                throw new IllegalStateException("src/main/resources/" + DB_PROPERTIES + " 파일이 없습니다.");
            }
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(DB_PROPERTIES + " 파일을 읽을 수 없습니다.", e);
        }

        for (String key : KEYS) {
            String override = System.getProperty(key);
            if (override != null) {
                properties.setProperty(key, override);
            }
        }

        if ("CHANGE_ME".equals(properties.getProperty("DB_PWD"))) {
            throw new IllegalStateException(
                    "src/main/resources/config/db.properties 의 DB_PWD 를 내 MySQL 비밀번호로 바꿔 주세요.");
        }
        return properties;
    }
}
