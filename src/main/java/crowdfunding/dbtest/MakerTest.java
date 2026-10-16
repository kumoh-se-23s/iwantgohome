package crowdfunding.dbtest;

import crowdfunding.config.AppConfig;
import crowdfunding.exception.BusinessException;
import crowdfunding.model.domain.Project;
import crowdfunding.model.domain.Reward;
import crowdfunding.model.enums.ApprovalStatus;
import crowdfunding.model.service.FundingStatusService;
import crowdfunding.model.service.MakerProjectService;
import crowdfunding.view.ResultView;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 메이커 검사 항목
 *   [검사 1] test01ViewFundingStatus : 프로젝트별 펀딩 현황 보기
 *   [검사 2] test02RegisterProject   : 프로젝트 기본 정보 등록
 *   [검사 3] test03RegisterReward    : 리워드 등록 후 프로젝트 신청
 *
 * 테스트는 Mapper 를 직접 부르지 않고 Service 를 호출한다.
 * → 12월 최종 시스템에서도 같은 Service 를 그대로 쓴다.
 *
 * [담당] D (검사 01), A (검사 02·03)
 */
@RequiredArgsConstructor
public class MakerTest {

    // ============================================================================
    //  검사 때 바꿔도 되는 값 (번호는 initDB 기준)
    // ============================================================================
    /** 검사하는 메이커: 2 = maker01 (김민수, 프로젝트 1·2·3·7) / 3 = maker02 (이수진) */
    private static final int MAKER_ID = 2;

    /** [검사 2] 등록할 프로젝트 */
    private static final String TEST02_NAME = "[검사] 휴대용 미니 가습기";
    private static final String TEST02_DESCRIPTION = "USB-C 충전식 300ml 미니 가습기, 무드등 겸용";
    private static final int TEST02_CATEGORY_ID = 1;           // 1 테크·가전, 2 푸드
    private static final int TEST02_TARGET_AMOUNT = 1_000_000;
    private static final int TEST02_PERIOD_DAYS = 30;          // 오늘부터 30일 동안 펀딩

    /** [검사 3] 리워드를 등록할 프로젝트. 0 이면 MAKER_ID 의 가장 최근 '작성 중' 프로젝트(= test02 결과)를 자동 선택 */
    private static final int TEST03_TARGET_PROJECT_ID = 0;
    // ============================================================================

    // 사용할 Service 들 - Main 에서 만들어서 생성자로 넣어 준다 (@RequiredArgsConstructor)
    private final FundingStatusService fundingStatusService;
    private final MakerProjectService makerProjectService;

    /** [검사 1] 프로젝트별 펀딩 현황 보기 (총 펀딩 금액, 리워드별 신청 수량/잔여 수량, 서포터 및 배송지) */
    public void test01ViewFundingStatus() {
        ResultView.title("[검사 1] MAKER - 프로젝트별 펀딩 현황 보기 (메이커 user " + MAKER_ID + ")");
        try {
            ResultView.fundingStatus(fundingStatusService.getFundingStatus(MAKER_ID));
        } catch (BusinessException e) {
            ResultView.fail(e.getMessage());
        }
        ResultView.checkSql(
                "SELECT f.project_id, SUM(r.reward_price) AS total_amount, COUNT(*) AS supporters FROM funding f "
                        + "JOIN reward r ON r.reward_id = f.reward_id JOIN project p ON p.project_id = f.project_id "
                        + "WHERE p.user_id = " + MAKER_ID + " GROUP BY f.project_id;",
                "SELECT r.project_id, r.reward_id, r.reward_name, r.applied_count, r.reward_amount, "
                        + "r.reward_amount - r.applied_count AS remaining, u.user_name, s.delivery_address "
                        + "FROM reward r JOIN project p ON p.project_id = r.project_id "
                        + "LEFT JOIN funding f ON f.reward_id = r.reward_id LEFT JOIN `user` u ON u.user_id = f.user_id "
                        + "LEFT JOIN supporter s ON s.user_id = f.user_id "
                        + "WHERE p.user_id = " + MAKER_ID + " AND p.approval_status = 'approved' "
                        + "ORDER BY r.project_id, r.reward_id;");
    }

    /** [검사 2] 프로젝트 기본 정보 등록 → approval_status = 'draft' (작성 중) */
    public void test02RegisterProject() {
        ResultView.title("[검사 2] MAKER - 프로젝트 기본 정보 등록 (메이커 user " + MAKER_ID + ")");

        LocalDate today = AppConfig.today();
        Project project = new Project();
        project.setProjectName(TEST02_NAME);
        project.setProjectDescription(TEST02_DESCRIPTION);
        project.setCategoryId(TEST02_CATEGORY_ID);
        project.setTargetAmount(TEST02_TARGET_AMOUNT);
        project.setStartDate(today);
        project.setEndDate(today.plusDays(TEST02_PERIOD_DAYS));

        try {
            int newId = makerProjectService.registerProject(MAKER_ID, project);
            ResultView.success("프로젝트 " + newId + "번 등록 → 리워드 등록 전이라 '작성 중(draft)' 상태");
            ResultView.project("등록 결과", makerProjectService.findProject(newId));
        } catch (BusinessException e) {
            ResultView.fail("프로젝트 등록  →  " + e.getMessage());
        }
        ResultView.checkSql("SELECT * FROM project ORDER BY project_id DESC LIMIT 1;");
    }

    /** [검사 3] 리워드 등록 후 프로젝트 신청 → reward 행 추가, approval_status = 'waiting' (승인 대기) */
    public void test03RegisterReward() {
        ResultView.title("[검사 3] MAKER - 리워드 등록 및 프로젝트 신청 (메이커 user " + MAKER_ID + ")");

        int projectId = resolveTargetProject();
        if (projectId == 0) {
            return;
        }
        Project project = makerProjectService.findProject(projectId);
        if (project == null) {
            ResultView.fail("프로젝트 " + projectId + "번이 없습니다. TEST03_TARGET_PROJECT_ID 를 확인하세요.");
            return;
        }
        ResultView.project("신청 전", project);

        // ① 리워드가 하나도 없을 때 신청하면 막혀야 한다 ("리워드 등록 후 프로젝트 신청")
        if (makerProjectService.findRewards(projectId).isEmpty()) {
            ResultView.section("① 리워드 없이 승인 신청 → 실패해야 정상");
            TestSupport.attempt("승인 신청", () -> makerProjectService.submitProject(MAKER_ID, projectId));
        }

        // ② 리워드 2개 등록 (1개는 수량 제한, 1개는 무제한). 제공 시기는 펀딩 종료 2주 뒤
        ResultView.section("② 리워드 등록");
        LocalDateTime delivery = project.getEndDate().plusDays(14).atStartOfDay();
        Reward limited = Reward.of("얼리버드 가습기", "가습기 1개 (얼리버드 할인)", 19_000, 10, delivery);
        Reward unlimited = Reward.of("가습기 기본", "가습기 1개", 25_000, null, delivery);
        TestSupport.attempt("리워드 등록: 얼리버드 가습기 (한정 10개)",
                () -> makerProjectService.addReward(MAKER_ID, projectId, limited));
        TestSupport.attempt("리워드 등록: 가습기 기본 (수량 무제한)",
                () -> makerProjectService.addReward(MAKER_ID, projectId, unlimited));
        for (Reward r : makerProjectService.findRewards(projectId)) {
            ResultView.reward("등록된 리워드", r);
        }

        // ③ 리워드 등록이 끝났으니 관리자에게 승인 신청
        ResultView.section("③ 프로젝트 승인 신청");
        TestSupport.attempt("승인 신청 (작성 중 → 승인 대기)",
                () -> makerProjectService.submitProject(MAKER_ID, projectId));
        ResultView.project("신청 후", makerProjectService.findProject(projectId));

        ResultView.checkSql(
                "SELECT * FROM reward WHERE project_id = " + projectId + ";",
                "SELECT project_id, project_name, approval_status, project_status FROM project WHERE project_id = "
                        + projectId + ";");
    }

    /** test03 대상 프로젝트 번호 정하기 */
    private int resolveTargetProject() {
        if (TEST03_TARGET_PROJECT_ID != 0) {
            return TEST03_TARGET_PROJECT_ID;
        }
        Integer latest = makerProjectService.findLatestProjectId(MAKER_ID, ApprovalStatus.DRAFT);
        if (latest == null) {
            ResultView.fail("작성 중인 프로젝트가 없습니다. test02 를 먼저 실행하거나 TEST03_TARGET_PROJECT_ID 에 번호를 넣으세요.");
            return 0;
        }
        ResultView.info("대상: 프로젝트 " + latest + "번 (가장 최근 '작성 중' 프로젝트 자동 선택)");
        return latest;
    }
}
