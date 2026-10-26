package crowdfunding.dbtest;

import crowdfunding.config.AppConfig;
import crowdfunding.config.PooledDataSource;
import crowdfunding.view.ResultView;
import org.apache.ibatis.jdbc.ScriptRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * initDB : 검사에 필요한 사전 데이터를 한 번에 DB 에 넣는다.
 *
 * - 데이터 내용은 src/main/resources/sql/seed.sql 에 SQL 로 적혀 있다.
 *   (자바 코드에 INSERT 를 수십 줄 넣는 것보다 고치기 쉽고, Workbench 에서 바로 실행해 볼 수도 있다)
 * - seed.sql 은 맨 앞에서 모든 테이블을 비우므로, 리허설을 몇 번 해도 initDB 한 번이면 처음 상태로 돌아온다.
 * - ScriptRunner 는 MyBatis 에 들어 있는 SQL 파일 실행기다.
 * - 커넥션은 PooledDataSource(DBCP 커넥션 풀)에서 하나 빌려 쓴다.
 *
 * [담당] B (initDB · 사전 데이터)
 */
public final class DataInitializer {

    private static final String SEED_SQL = "sql/seed.sql";

    private DataInitializer() {
    }

    public static void initDB() {
        LocalDate today = AppConfig.today();

        InputStream in = DataInitializer.class.getClassLoader().getResourceAsStream(SEED_SQL);
        if (in == null) {
            throw new IllegalStateException("src/main/resources/" + SEED_SQL + " 파일이 없습니다.");
        }

        try (Connection conn = PooledDataSource.getDataSource().getConnection();
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {

            // seed.sql 의 날짜 기준(@today)을 자바의 '오늘'(한국 시간)과 맞춘다
            try (PreparedStatement ps = conn.prepareStatement("SET @today = ?")) {
                ps.setString(1, today.toString());
                ps.execute();
            }

            ScriptRunner runner = new ScriptRunner(conn);
            runner.setStopOnError(true);   // 한 문장이라도 실패하면 즉시 멈춘다
            runner.setRemoveCRs(true);     // 윈도우 줄바꿈(\r\n) 정리
            runner.setLogWriter(null);     // 실행한 SQL 을 전부 출력하지 않음
            runner.runScript(reader);      // 스크립트 마지막에 commit 된다

            printSummary(conn);
            ResultView.success("사전 데이터 입력 완료.");

        } catch (IOException | SQLException e) {
            throw new RuntimeException("initDB 실패: " + e.getMessage(), e);
        }
    }

    /** 테이블별 행 수 출력 */
    private static void printSummary(Connection conn) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM `user`), (SELECT COUNT(*) FROM supporter),"
                + " (SELECT COUNT(*) FROM category), (SELECT COUNT(*) FROM project),"
                + " (SELECT COUNT(*) FROM reward), (SELECT COUNT(*) FROM funding),"
                + " (SELECT COUNT(*) FROM project_like), (SELECT COUNT(*) FROM review)";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            ResultView.info(String.format(
                    "사용자 %d명 (서포터 %d) | 카테고리 %d | 프로젝트 %d | 리워드 %d | 후원 %d | 좋아요 %d | 리뷰 %d",
                    rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4),
                    rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8)));
        }
    }
}
