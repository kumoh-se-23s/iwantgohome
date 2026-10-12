package crowdfunding.config;

import java.time.LocalDate;
import java.time.ZoneId;

/** 프로그램 전역 설정값 */
public final class AppConfig {

    private AppConfig() {
    }

    /** 2차 TCP 서버 기본 포트 */
    public static final int SERVER_PORT = 9000;

    /** 2차 클라이언트 기본 접속 주소 (시연 때는 서버 노트북 IP 를 실행 인자로 넘김) */
    public static final String DEFAULT_HOST = "localhost";

    /** 종료일이 지난 프로젝트를 성공/실패 처리하는 주기 (2차 서버) */
    public static final int CLOSE_CHECK_INTERVAL_SECONDS = 60;

    /** 리워드 이미지 최대 크기 — DB CHECK(ck_reward_image)와 같은 5MB */
    public static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    /** 프로젝트를 승인 신청하려면 필요한 최소 리워드 수 */
    public static final int MIN_REWARDS_TO_SUBMIT = 1;

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 오늘 날짜. 날짜 판단(펀딩중/마감, 신청 가능 여부)은 모두 이 메소드 하나를 기준으로 한다.
     * DB 의 CURDATE() 대신 이 값을 쿼리 파라미터로 넘겨서 자바/DB 기준이 어긋나지 않게 한다.
     */
    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
