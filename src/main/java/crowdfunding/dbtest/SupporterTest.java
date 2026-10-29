package crowdfunding.dbtest;

import crowdfunding.exception.BusinessException;
import crowdfunding.model.dto.MyInfoForm;
import crowdfunding.model.dto.ProjectSearchCondition;
import crowdfunding.model.service.FundingService;
import crowdfunding.model.service.FundingStatusService;
import crowdfunding.model.service.MyPageService;
import crowdfunding.model.service.ProjectSearchService;
import crowdfunding.model.service.ReviewService;
import crowdfunding.view.ResultView;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 서포터 검사 항목
 *   [검사 5]  test05UpdateProfile      : (MyPage) 개인정보 수정
 *   [검사 6]  test06ViewProjects       : 프로젝트 보기(기본)
 *   [검사 7]  test07FilterProjects     : 프로젝트 필터링 (카테고리, 진행/마감)
 *   [검사 8]  test08ApplyReward        : 리워드 신청 (프로젝트당 리워드 1개)
 *   [검사 9]  test09RegisterReview     : 리뷰와 별점 등록 (마감 + 펀딩 성공 프로젝트)
 *   [검사 10] test10ConcurrencyControl : 제공 가능 수량 동시성 제어
 *   (선택)    test10bConcurrencyWithThreads : 실제 스레드로 동시에 신청
 *
 * [담당] D (검사 05), B (검사 06·07), C (검사 08·09·10)
 */
@RequiredArgsConstructor
public class SupporterTest {

    // ============================================================================
    //  검사 때 바꿔도 되는 값 (번호는 initDB 기준)
    // ============================================================================
    /** [검사 5] 개인정보를 수정할 서포터: 11 = sup08 (다른 검사에서 쓰지 않는 계정) */
    private static final int TEST05_USER_ID = 11;
    private static final String TEST05_NEW_NAME = "한소율";
    private static final String TEST05_NEW_PHONE = "010-9999-8888";
    private static final String TEST05_CURRENT_PW = "pass1234";      // 현재 비밀번호 (initDB 기준)
    private static final String TEST05_NEW_PW = "newpass5678";       // null 이면 비밀번호는 유지
    private static final String TEST05_NEW_ADDRESS = "경상북도 구미시 구미대로 200, 305호";
    private static final LocalDate TEST05_NEW_BIRTH = LocalDate.of(2004, 6, 8);

    /** [검사 7] 필터 조건 */
    private static final Integer TEST07_CATEGORY_ID = 1;             // 1 테크·가전, 2 푸드, null = 전체
    private static final String TEST07_STATUS = "IN_PROGRESS";       // "IN_PROGRESS"(펀딩중) / "CLOSED"(펀딩종료) / null = 전체
    private static final String TEST07_KEYWORD = null;               // 프로젝트명 검색어 (null = 없음)

    /** [검사 8] 리워드 신청 */
    private static final int TEST08_SUPPORTER_ID = 4;                // sup01 (박준호)
    private static final int TEST08_PROJECT_ID = 4;                  // 제주 감귤 착즙 주스 (maker02, 펀딩중)
    private static final int TEST08_REWARD_ID = 8;                   // 착즙 주스 30병 세트 (한정 10개 중 1개 신청됨)
    private static final int TEST08_OTHER_REWARD_ID = 7;             // 같은 프로젝트의 다른 리워드 → 재신청 막히는지 확인

    /** [검사 9] 리뷰 등록 */
    private static final int TEST09_SUPPORTER_ID = 4;                // sup01 (2번 프로젝트 후원함)
    private static final int TEST09_PROJECT_ID = 2;                  // 수제 그래놀라 정기배송 (마감 + 펀딩 성공)
    private static final int TEST09_RATING = 5;                      // 1 ~ 5
    private static final String TEST09_CONTENT = "매주 신선하게 와서 좋아요. 정기배송 연장할게요!";
    private static final int TEST09_FAIL_SUPPORTER_ID = 7;           // sup04 (3번 프로젝트 후원함)
    private static final int TEST09_FAIL_PROJECT_ID = 3;             // 휴대용 미니 빔프로젝터 (마감 + 펀딩 실패) → 막혀야 정상

    /** [검사 10] 수량 동시성: 1번 리워드는 수량 3개 중 2개 신청됨 → 잔여 1개 */
    private static final int TEST10_PROJECT_ID = 1;                  // 접이식 블루투스 키보드 (펀딩중)
    private static final int TEST10_REWARD_ID = 1;                   // 얼리버드 키보드
    private static final int TEST10_SUPPORTER_A = 8;                 // sup05 → 마지막 1개 신청 → 성공해야 함
    private static final int TEST10_SUPPORTER_B = 9;                 // sup06 → 수량 부족 → 실패해야 함

    /** (선택) 실제 동시 신청: 9번 리워드 한정 2개(신청 0개)에 6명이 동시에 */
    private static final int TEST10B_PROJECT_ID = 5;                 // 스마트 식물 재배기
    private static final int TEST10B_REWARD_ID = 9;                  // 식물 재배기 1대 (한정 2개)
    private static final int[] TEST10B_SUPPORTERS = {4, 5, 6, 7, 8, 9};
    // ============================================================================

    // 사용할 Service 들 - Main 에서 만들어서 생성자로 넣어 준다 (@RequiredArgsConstructor)
    private final MyPageService myPageService;
    private final ProjectSearchService projectSearchService;
    private final FundingService fundingService;
    private final FundingStatusService fundingStatusService;
    private final ReviewService reviewService;

    /** [검사 5] MyPage 개인정보 수정 → user(이름·연락처·비밀번호) + supporter(배송지·생년월일) */
    public void test05UpdateProfile() {
        ResultView.title("[검사 5] SUPPORTER - (MyPage) 개인정보 수정 (서포터 user " + TEST05_USER_ID + ")");

        ResultView.profile("수정 전", myPageService.findUser(TEST05_USER_ID), myPageService.findSupporter(TEST05_USER_ID));
        MyInfoForm form = new MyInfoForm(TEST05_NEW_NAME, TEST05_NEW_PHONE, TEST05_CURRENT_PW, TEST05_NEW_PW,
                TEST05_NEW_ADDRESS, TEST05_NEW_BIRTH);
        TestSupport.attempt("개인정보 수정",
                () -> myPageService.updateMyInfo(TEST05_USER_ID, form)); //(user · supporter 두 테이블을 한 트랜잭션으로)
        ResultView.profile("수정 후", myPageService.findUser(TEST05_USER_ID), myPageService.findSupporter(TEST05_USER_ID));

        ResultView.checkSql("SELECT u.user_id, u.user_name, u.phone_number, u.pw, s.delivery_address, s.birth_date "
                + "FROM `user` u JOIN supporter s ON s.user_id = u.user_id WHERE u.user_id = " + TEST05_USER_ID + ";");
    }

    /** [검사 6] 프로젝트 보기 (기본: 조건 없이 승인된 프로젝트 전체, 최신순) */
    public void test06ViewProjects() {
        ResultView.title("[검사 6] SUPPORTER - 프로젝트 보기 (승인된 프로젝트만, 최신순)");
        //승인 대기·거절된 프로젝트는 서포터에게 보이지 않아야 정상
        ResultView.projects(projectSearchService.search(new ProjectSearchCondition()));
        ResultView.checkSql("SELECT project_id, project_name, approval_status, project_status, end_date "
                + "FROM project ORDER BY created_at DESC;");
    }

    /** [검사 7] 프로젝트 필터링 (카테고리, 진행/마감) - MyBatis 동적 쿼리 */
    public void test07FilterProjects() {
        ResultView.title("[검사 7] SUPPORTER - 프로젝트 필터링 (MyBatis 동적 쿼리)");

        ProjectSearchCondition condition =
                new ProjectSearchCondition(TEST07_CATEGORY_ID, TEST07_STATUS, TEST07_KEYWORD);
        ResultView.info("조건: " + condition.describe());
        try {
            ResultView.projects(projectSearchService.search(condition));
        } catch (BusinessException e) {
            ResultView.fail(e.getMessage());
        }
        ResultView.checkSql("SELECT p.project_id, p.project_name, p.category_id, p.project_status, p.end_date "
                + "FROM project p WHERE p.approval_status = 'approved'"
                + (TEST07_CATEGORY_ID == null ? "" : " AND p.category_id = " + TEST07_CATEGORY_ID)
                + ("IN_PROGRESS".equals(TEST07_STATUS) ? " AND p.project_status = 'in_progress' AND p.end_date >= CURDATE()" : "")
                + ("CLOSED".equals(TEST07_STATUS) ? " AND (p.project_status IN ('success','fail') OR p.end_date < CURDATE())" : "")
                + " ORDER BY p.created_at DESC;");
    }

    /**
     * [검사 8] 리워드 신청
     *   - 리워드 수량이 줄어드는가
     *   - 펀딩 기록이 남는가
     *   - (메이커 입장에서) 나의 프로젝트에 누가 펀딩했는지 확인할 수 있는가
     *   - 하나의 프로젝트에는 하나의 리워드만 신청 가능한가
     */
    public void test08ApplyReward() {
        ResultView.title("[검사 8] SUPPORTER - 리워드 신청 (서포터 user " + TEST08_SUPPORTER_ID + " → 프로젝트 "
                + TEST08_PROJECT_ID + ", 리워드 " + TEST08_REWARD_ID + ")");

        ResultView.section("① 신청 전");
        ResultView.reward("리워드", fundingService.findReward(TEST08_REWARD_ID));
        ResultView.funding("후원 기록", fundingService.findFunding(TEST08_SUPPORTER_ID, TEST08_PROJECT_ID));

        ResultView.section("② 리워드 신청");
        TestSupport.attempt("리워드 " + TEST08_REWARD_ID + " 신청",
                () -> fundingService.applyReward(TEST08_SUPPORTER_ID, TEST08_PROJECT_ID, TEST08_REWARD_ID));

        ResultView.section("③ 신청 후 - 리워드 수량이 줄었는가 / 펀딩 기록이 남았는가");
        ResultView.reward("리워드", fundingService.findReward(TEST08_REWARD_ID));
        ResultView.funding("후원 기록", fundingService.findFunding(TEST08_SUPPORTER_ID, TEST08_PROJECT_ID));

        ResultView.section("④ 메이커 화면 - 나의 프로젝트에 누가 펀딩했는가");
        try {
            int makerId = fundingStatusService.findMakerIdOf(TEST08_PROJECT_ID);
            ResultView.info("프로젝트 " + TEST08_PROJECT_ID + "번의 메이커(user " + makerId + ")로 펀딩 현황 조회");
            ResultView.fundingStatus(fundingStatusService.getFundingStatus(makerId, TEST08_PROJECT_ID));
        } catch (BusinessException e) {
            ResultView.fail(e.getMessage());
        }

        ResultView.section("⑤ 같은 프로젝트의 다른 리워드로 다시 신청 → 실패해야 정상 (프로젝트당 리워드 1개)");
        TestSupport.attempt("리워드 " + TEST08_OTHER_REWARD_ID + " 신청",
                () -> fundingService.applyReward(TEST08_SUPPORTER_ID, TEST08_PROJECT_ID, TEST08_OTHER_REWARD_ID));
        ResultView.reward("리워드 (변화 없어야 함)", fundingService.findReward(TEST08_OTHER_REWARD_ID));

        ResultView.checkSql(
                "SELECT reward_id, reward_name, reward_amount, applied_count, reward_amount - applied_count AS remaining "
                        + "FROM reward WHERE project_id = " + TEST08_PROJECT_ID + ";",
                "SELECT f.*, u.user_name, s.delivery_address FROM funding f JOIN `user` u ON u.user_id = f.user_id "
                        + "JOIN supporter s ON s.user_id = f.user_id WHERE f.project_id = " + TEST08_PROJECT_ID + ";");
    }

    /** [검사 9] 리뷰와 별점(1~5) 등록 - 마감된 프로젝트 + 펀딩이 성공한 프로젝트 (리워드명/별점/리뷰) */
    public void test09RegisterReview() {
        ResultView.title("[검사 9] SUPPORTER - 리뷰와 별점 등록 (마감 + 펀딩 성공 프로젝트만)");

        ResultView.section("① 마감 + 펀딩 성공한 프로젝트 " + TEST09_PROJECT_ID + "번에 리뷰 등록 (서포터 user "
                + TEST09_SUPPORTER_ID + ")");
        boolean ok = TestSupport.attempt("리뷰 등록 (별점 " + TEST09_RATING + "점)",
                () -> reviewService.writeReview(TEST09_SUPPORTER_ID, TEST09_PROJECT_ID, TEST09_RATING, TEST09_CONTENT));
        if (ok) {
            ResultView.section("등록된 리뷰 - 리워드명 / 별점 / 리뷰");
            ResultView.review(reviewService.findMyReview(TEST09_SUPPORTER_ID, TEST09_PROJECT_ID));
        }

        ResultView.section("② 마감됐지만 펀딩에 실패한 프로젝트 " + TEST09_FAIL_PROJECT_ID + "번 → 실패해야 정상 (서포터 user "
                + TEST09_FAIL_SUPPORTER_ID + ")");
        TestSupport.attempt("리뷰 등록",
                () -> reviewService.writeReview(TEST09_FAIL_SUPPORTER_ID, TEST09_FAIL_PROJECT_ID, 2, "실패한 프로젝트 리뷰"));

        ResultView.checkSql(
                "SELECT v.review_id, p.project_name, r.reward_name, v.rating, v.review_content, v.review_at "
                        + "FROM review v JOIN funding f ON f.user_id = v.user_id AND f.project_id = v.project_id "
                        + "JOIN reward r ON r.reward_id = f.reward_id JOIN project p ON p.project_id = v.project_id "
                        + "WHERE v.project_id IN (" + TEST09_PROJECT_ID + ", " + TEST09_FAIL_PROJECT_ID + ") "
                        + "ORDER BY v.review_at DESC;");
    }

    /**
     * [검사 10] 제공 가능 수량 동시성 제어
     *   케이스 1. 수량이 충분 → 펀딩 성공 : 서포터의 펀딩 기록이 남고, 재고(applied_count)가 바뀐다
     *   케이스 2. 수량이 부족 → 펀딩 실패 : 펀딩 기록이 남지 않고, 재고도 바뀌지 않는다 (트랜잭션 롤백)
     */
    public void test10ConcurrencyControl() {
        ResultView.title("[검사 10] SUPPORTER - 제공 가능 수량 동시성 제어 (리워드 " + TEST10_REWARD_ID + ")");

        ResultView.section("신청 전");
        printRewardState(TEST10_REWARD_ID);

        ResultView.section("케이스 1. 수량이 충분 → 펀딩 성공 (서포터 user " + TEST10_SUPPORTER_A + ")");
        TestSupport.attempt("리워드 신청",
                () -> fundingService.applyReward(TEST10_SUPPORTER_A, TEST10_PROJECT_ID, TEST10_REWARD_ID));
        printRewardState(TEST10_REWARD_ID);
        ResultView.funding("user " + TEST10_SUPPORTER_A + " 후원 기록",
                fundingService.findFunding(TEST10_SUPPORTER_A, TEST10_PROJECT_ID));

        ResultView.section("케이스 2. 수량이 부족 → 펀딩 실패 (서포터 user " + TEST10_SUPPORTER_B + ")");
        TestSupport.attempt("리워드 신청",
                () -> fundingService.applyReward(TEST10_SUPPORTER_B, TEST10_PROJECT_ID, TEST10_REWARD_ID));
        printRewardState(TEST10_REWARD_ID);
        ResultView.funding("user " + TEST10_SUPPORTER_B + " 후원 기록",
                fundingService.findFunding(TEST10_SUPPORTER_B, TEST10_PROJECT_ID));

        ResultView.checkSql(
                "SELECT reward_id, reward_name, reward_amount, applied_count FROM reward WHERE reward_id = "
                        + TEST10_REWARD_ID + ";",
                "SELECT * FROM funding WHERE reward_id = " + TEST10_REWARD_ID + " ORDER BY funded_date;");
    }

    /**
     * (선택) 실제 동시 신청 테스트 - 질문을 받으면 보여 줄 용도
     * 잔여 2개인 리워드에 서포터 6명이 "동시에" 신청 → 정확히 2명만 성공해야 한다.
     * CountDownLatch 로 모든 스레드를 출발선에 세웠다가 한 번에 출발시킨다.
     */
    public void test10bConcurrencyWithThreads() {
        ResultView.title("[검사 10 · 추가] 실제 동시 신청 - 리워드 " + TEST10B_REWARD_ID + "에 서포터 "
                + TEST10B_SUPPORTERS.length + "명이 동시에 신청");

        ResultView.section("신청 전");
        printRewardState(TEST10B_REWARD_ID);

        int n = TEST10B_SUPPORTERS.length;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);   // 모든 스레드가 준비됐는지
        CountDownLatch start = new CountDownLatch(1);   // 출발 신호
        List<Future<String>> results = new ArrayList<>();

        for (int userId : TEST10B_SUPPORTERS) {
            results.add(pool.submit(() -> {
                ready.countDown();
                start.await();   // 출발 신호가 올 때까지 대기 → 모두 동시에 출발
                try {
                    fundingService.applyReward(userId, TEST10B_PROJECT_ID, TEST10B_REWARD_ID);
                    return "user " + userId + " : 성공";
                } catch (BusinessException e) {
                    return "user " + userId + " : 실패 (" + e.getMessage() + ")";
                }
            }));
        }

        ResultView.section("동시 신청 결과");
        int successCount = 0;
        try {
            ready.await();
            start.countDown();   // 출발!
            for (Future<String> f : results) {
                String result = f.get();
                if (result.contains("성공")) {
                    successCount++;
                }
                ResultView.info(result);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            pool.shutdown();
        }

        ResultView.section("신청 후");
        printRewardState(TEST10B_REWARD_ID);
        ResultView.info("성공 " + successCount + "명 (기대값: 잔여 수량만큼만 성공)");
    }

    /** 리워드 수량 상태와 실제 후원 기록 수를 같이 출력 (두 값이 항상 같아야 정상) */
    private void printRewardState(int rewardId) {
        ResultView.reward("리워드", fundingService.findReward(rewardId));
        ResultView.info("이 리워드의 후원 기록: " + fundingService.countFundingsOfReward(rewardId) + "건");
    }
}