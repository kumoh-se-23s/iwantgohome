package crowdfunding.config;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 프로그램 전체에서 쓰는 설정값과 업무 규칙 숫자를 한곳에 모아 둔 클래스.
 * 규칙을 바꿔야 하면 여기 숫자만 고치면 된다.
 *
 * [담당] 공통 기반
 */
public final class AppConfig {

    /** 날짜 판단 기준 시간대. DB 서버 시간대와 상관없이 한국 날짜로 판단한다. */
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    /** 구현 범위 2.1 - 각 프로젝트에는 최소 2개 이상의 리워드를 제공한다 → 2개 이상이어야 승인 신청 가능 */
    public static final int MIN_REWARDS_TO_SUBMIT = 2;

    /** 구현 범위 2.2 - 하나 이상의 리워드는 제공 가능한 수량이 제한되어 있어야 한다 → 신청할 때 검사 */
    public static final boolean REQUIRE_LIMITED_REWARD = true;

    private AppConfig() {
    }

    /**
     * 오늘 날짜 (한국 시간).
     * 리허설에서 다른 날짜를 흉내 내고 싶으면 실행 옵션 -Dapp.today=2026-11-02 를 준다.
     */
    public static LocalDate today() {
        String fixed = System.getProperty("app.today");
        return fixed != null ? LocalDate.parse(fixed) : LocalDate.now(ZONE);
    }
}
