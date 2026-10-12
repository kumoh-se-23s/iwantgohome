package crowdfunding.dbtest;

import crowdfunding.model.domain.Funding;
import crowdfunding.model.domain.Review;
import crowdfunding.model.domain.Reward;
import crowdfunding.model.dto.LoginForm;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProfileForm;
import crowdfunding.model.dto.ProjectDetail;
import crowdfunding.model.dto.ProjectSearchCondition;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.model.dto.SupporterProfile;
import crowdfunding.model.enums.FundingState;
import crowdfunding.model.enums.ProjectSort;
import crowdfunding.protocol.Response;
import crowdfunding.view.ResultView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/** 1차 검사 — 서포터 (검사 5 ~ 10) */
public class SupporterTest extends DbTestSupport {

    // ===== 검사 시 수정 가능한 값 =====
    /** 검사 5 대상 서포터 — 비밀번호가 바뀌므로 다른 검사에서 쓰지 않는 sup08 사용 */
    private static final String TEST05_SUPPORTER_ID = "sup08";
    private static final String TEST05_NEW_PASSWORD = "newpass5678";

    /** 검사 6~9 를 수행하는 서포터 */
    private static final String SUPPORTER_ID = "sup01";

    /** 검사 7 필터 조건 (카테고리: 1=테크·가전, 2=푸드 / 상태: OPEN=펀딩중, CLOSED=펀딩종료) */
    private static final Integer TEST07_CATEGORY_1 = DataInitializer.CATEGORY_TECH;
    private static final FundingState TEST07_STATE_1 = FundingState.OPEN;
    private static final Integer TEST07_CATEGORY_2 = DataInitializer.CATEGORY_FOOD;
    private static final FundingState TEST07_STATE_2 = FundingState.CLOSED;

    /** 검사 8 신청 대상 (4번 '제주 감귤 착즙 주스' / 7번 리워드, 두 번째 시도는 같은 프로젝트의 8번 리워드) */
    private static final int TEST08_PROJECT_ID = DataInitializer.P_JUICE;
    private static final int TEST08_REWARD_ID = DataInitializer.R_JUICE_10;
    private static final int TEST08_SECOND_REWARD_ID = DataInitializer.R_JUICE_30;

    /** 검사 9 리뷰 대상 (2번 '수제 그래놀라' — 펀딩 성공, sup01 이 후원함) */
    private static final int TEST09_PROJECT_ID = DataInitializer.P_GRANOLA;

    /** 검사 10 대상 (1번 프로젝트의 1번 리워드 — 한정 3개 중 2개 신청됨, 잔여 1개) */
    private static final int TEST10_PROJECT_ID = DataInitializer.P_KEYBOARD;
    private static final int TEST10_REWARD_ID = DataInitializer.R_KEYBOARD_EARLY;
    private static final String TEST10_FIRST_SUPPORTER = "sup04";    // 잔여 1개 → 성공
    private static final String TEST10_SECOND_SUPPORTER = "sup05";   // 잔여 0개 → 실패

    /** 검사 5. (MyPage) 개인정보 수정 — user, supporter 두 테이블이 한 트랜잭션으로 수정됨 */
    public void test05UpdateProfile() {
        header("05", "SUPPORTER", "개인정보 수정");
        LoginUser supporter = login(TEST05_SUPPORTER_ID, DataInitializer.SUPPORTER_PW);

        ResultView.section("수정 전");
        ResultView.printProfile(ctx.supporter().getProfile(supporter).getData());

        ProfileForm form = new ProfileForm(
                "임지우",                                       // 이름
                "010-9999-0008",                                // 연락처
                "경상북도 구미시 대학로 61 디지털관 215호",       // 배송지
                LocalDate.of(2002, 7, 7))                       // 생년월일
                .changePassword(DataInitializer.SUPPORTER_PW, TEST05_NEW_PASSWORD);
        System.out.println();
        ResultView.printResponse(ctx.supporter().updateProfile(supporter, form));

        ResultView.section("수정 후");
        Response<SupporterProfile> after = ctx.supporter().getProfile(supporter);
        ResultView.printProfile(after.getData());
        System.out.println("  새 비밀번호로 로그인 확인 → "
                + ctx.auth().login(new LoginForm(TEST05_SUPPORTER_ID, TEST05_NEW_PASSWORD)));
        checkSql("SELECT u.user_id, u.id, u.pw, u.user_name, u.phone_number, s.delivery_address, s.birth_date",
                "  FROM `user` u JOIN supporter s ON s.user_id = u.user_id WHERE u.id = '" + TEST05_SUPPORTER_ID + "';");
    }

    /** 검사 6. 프로젝트 보기(기본) — 승인된 프로젝트 전체, 최신순 */
    public void test06ViewProjects() {
        header("06", "SUPPORTER", "프로젝트 보기");
        LoginUser supporter = login(SUPPORTER_ID, DataInitializer.SUPPORTER_PW);

        Response<List<ProjectSummary>> res = ctx.project().searchProjects(supporter, null);
        ResultView.printResponse(res);
        ResultView.printProjectList(res.getData());
        checkSql("SELECT project_id, project_name, approval_status, project_status, end_date FROM project",
                "  WHERE approval_status = 'approved' ORDER BY created_at DESC;");
    }

    /** 검사 7. 프로젝트 필터링 (카테고리, 진행/마감) — MyBatis 동적 쿼리 */
    public void test07FilterProjects() {
        header("07", "SUPPORTER", "프로젝트 필터링");
        LoginUser supporter = login(SUPPORTER_ID, DataInitializer.SUPPORTER_PW);

        filter(supporter, new ProjectSearchCondition(null, TEST07_CATEGORY_1, TEST07_STATE_1, ProjectSort.LATEST));
        filter(supporter, new ProjectSearchCondition(null, TEST07_CATEGORY_2, TEST07_STATE_2, ProjectSort.LATEST));
        checkSql("SELECT project_id, project_name, category_id, project_status, end_date FROM project",
                "  WHERE approval_status = 'approved' ORDER BY category_id, project_status;");
    }

    private void filter(LoginUser supporter, ProjectSearchCondition condition) {
        ResultView.section("조건: " + condition);
        Response<List<ProjectSummary>> res = ctx.project().searchProjects(supporter, condition);
        ResultView.printResponse(res);
        ResultView.printProjectList(res.getData());
    }

    /** 검사 8. 리워드 신청 — 한 프로젝트에는 하나의 리워드(후원)만 신청 가능 */
    public void test08ApplyReward() {
        header("08", "SUPPORTER", "리워드 신청");
        LoginUser supporter = login(SUPPORTER_ID, DataInitializer.SUPPORTER_PW);

        Response<ProjectDetail> detail = ctx.project().getProjectDetail(supporter, TEST08_PROJECT_ID);
        if (detail.isSuccess()) {
            ResultView.printProjectDetail(detail.getData());
        }

        System.out.println("\n(1) " + TEST08_PROJECT_ID + "번 프로젝트 / " + TEST08_REWARD_ID + "번 리워드 신청");
        printRewardQuantity(supporter, TEST08_PROJECT_ID, TEST08_REWARD_ID, "신청 전");
        ResultView.printResponse(ctx.supporter().applyReward(supporter, new Funding(TEST08_PROJECT_ID, TEST08_REWARD_ID)));
        printRewardQuantity(supporter, TEST08_PROJECT_ID, TEST08_REWARD_ID, "신청 후");

        System.out.println("\n(2) 같은 프로젝트의 다른 리워드(" + TEST08_SECOND_REWARD_ID + "번) 신청 — 실패해야 함");
        ResultView.printResponse(ctx.supporter().applyReward(supporter, new Funding(TEST08_PROJECT_ID, TEST08_SECOND_REWARD_ID)));
        printRewardQuantity(supporter, TEST08_PROJECT_ID, TEST08_SECOND_REWARD_ID, "변화 없음");

        checkSql("SELECT * FROM funding WHERE project_id = " + TEST08_PROJECT_ID + ";",
                "SELECT reward_id, reward_name, reward_amount, applied_count FROM reward WHERE project_id = " + TEST08_PROJECT_ID + ";");
    }

    /** 검사 9. 리뷰와 별점(1~5점) 등록 — 리워드명 / 별점 / 리뷰 */
    public void test09RegisterReview() {
        header("09", "SUPPORTER", "리뷰 및 별점 등록");
        LoginUser supporter = login(SUPPORTER_ID, DataInitializer.SUPPORTER_PW);

        System.out.println("(1) 펀딩 성공 프로젝트(" + TEST09_PROJECT_ID + "번)에 리뷰 등록");
        ResultView.printResponse(ctx.supporter().registerReview(supporter,
                new Review(TEST09_PROJECT_ID, 5, "3개월분으로 신청했는데 매번 갓 구운 것처럼 바삭해요. 재구매 의사 있습니다!")));

        System.out.println("\n(2) 아직 펀딩 중인 프로젝트(" + DataInitializer.P_KEYBOARD + "번)에 리뷰 시도 — 실패해야 함");
        ResultView.printResponse(ctx.supporter().registerReview(supporter,
                new Review(DataInitializer.P_KEYBOARD, 4, "아직 못 받았지만 기대돼요")));

        ResultView.section(TEST09_PROJECT_ID + "번 프로젝트 리뷰 (리워드명 / 별점 / 리뷰, 최신순)");
        ResultView.printReviews(ctx.project().getProjectReviews(supporter, TEST09_PROJECT_ID).getData());
        checkSql("SELECT * FROM review WHERE project_id = " + TEST09_PROJECT_ID + " ORDER BY review_at DESC;");
    }

    /**
     * 검사 10. 제공 가능 수량 동시성 제어
     * 수량이 남았을 때 신청 → 펀딩 기록 생김 + 재고(applied_count) 수정
     * 수량이 없을 때 신청   → 펀딩 기록 없음 + 재고 그대로 (트랜잭션 롤백)
     */
    public void test10ConcurrencyControl() {
        header("10", "SUPPORTER", "제공 가능 수량 동시성 제어");
        LoginUser first = login(TEST10_FIRST_SUPPORTER, DataInitializer.SUPPORTER_PW);
        LoginUser second = login(TEST10_SECOND_SUPPORTER, DataInitializer.SUPPORTER_PW);

        printRewardQuantity(first, TEST10_PROJECT_ID, TEST10_REWARD_ID, "처음");

        System.out.println("\n(1) " + TEST10_FIRST_SUPPORTER + " 신청 (잔여 수량 있음) — 성공해야 함");
        ResultView.printResponse(ctx.supporter().applyReward(first, new Funding(TEST10_PROJECT_ID, TEST10_REWARD_ID)));
        printRewardQuantity(first, TEST10_PROJECT_ID, TEST10_REWARD_ID, "1차 후");

        System.out.println("\n(2) " + TEST10_SECOND_SUPPORTER + " 신청 (잔여 수량 없음) — 실패해야 함");
        ResultView.printResponse(ctx.supporter().applyReward(second, new Funding(TEST10_PROJECT_ID, TEST10_REWARD_ID)));
        printRewardQuantity(first, TEST10_PROJECT_ID, TEST10_REWARD_ID, "2차 후");

        checkSql("SELECT reward_id, reward_name, reward_amount, applied_count FROM reward WHERE reward_id = " + TEST10_REWARD_ID + ";",
                "SELECT f.*, u.id FROM funding f JOIN `user` u ON u.user_id = f.user_id WHERE f.reward_id = "
                        + TEST10_REWARD_ID + ";   -- " + TEST10_SECOND_SUPPORTER + " 의 기록이 없어야 함");
    }

    /**
     * (선택) 검사 10-B. 실제 동시 요청 테스트 — 2차 데모(요구사항 4.3) 대비
     * 잔여 2개인 리워드에 서포터 6명이 '동시에' 신청 → 정확히 2명만 성공, applied_count = 2
     * 각 스레드는 2차 서버의 ClientHandler 스레드와 똑같이 컨트롤러를 호출한다.
     */
    public void test10bConcurrencyWithThreads() {
        header("10-B", "SUPPORTER", "스레드 동시 신청 테스트 (선택)");
        int projectId = DataInitializer.P_PLANTER;
        int rewardId = DataInitializer.R_PLANTER_LIMITED;
        String[] supporterIds = {"sup01", "sup02", "sup03", "sup04", "sup05", "sup06"};

        List<LoginUser> users = new ArrayList<>();
        for (String id : supporterIds) {
            users.add(login(id, DataInitializer.SUPPORTER_PW));
        }
        printRewardQuantity(users.get(0), projectId, rewardId, "시작 전");

        CountDownLatch ready = new CountDownLatch(users.size());
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (LoginUser user : users) {
            Thread t = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();   // 모든 스레드가 준비되면 동시에 출발
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                Response<Reward> res = ctx.supporter().applyReward(user, new Funding(projectId, rewardId));
                if (res.isSuccess()) {
                    success.incrementAndGet();
                }
                System.out.println("  [" + Thread.currentThread().getName() + "] " + user.getLoginId() + " → " + res);
            }, "요청-" + user.getLoginId());
            threads.add(t);
            t.start();
        }
        try {
            ready.await();
            start.countDown();
            for (Thread t : threads) {
                t.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n  동시 요청 " + users.size() + "건 중 성공 " + success.get() + "건 (기대값: 2건)");
        printRewardQuantity(users.get(0), projectId, rewardId, "종료 후");
        checkSql("SELECT reward_id, reward_amount, applied_count FROM reward WHERE reward_id = " + rewardId + ";",
                "SELECT * FROM funding WHERE reward_id = " + rewardId + ";");
    }
}
