package crowdfunding.dbtest;

import crowdfunding.config.AppConfig;
import crowdfunding.model.domain.Project;
import crowdfunding.model.domain.Reward;
import crowdfunding.model.dto.FundingStatus;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.model.enums.ApprovalStatus;
import crowdfunding.protocol.Response;
import crowdfunding.view.ResultView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** 1차 검사 — 메이커 (검사 1, 2, 3) */
public class MakerTest extends DbTestSupport {

    // ===== 검사 시 수정 가능한 값 =====
    /** 검사 1: 특정 프로젝트만 보려면 번호 입력 (null = maker01 의 승인된 프로젝트 전부) */
    private static final Integer TEST01_PROJECT_ID = null;
    /** 검사 3: 리워드를 등록할 프로젝트 번호 (null = maker01 의 가장 최근 '작성 중' 프로젝트 = 검사 2에서 만든 것) */
    private static final Integer TEST03_PROJECT_ID = null;

    /**
     * 검사 1. 프로젝트별 펀딩 현황 보기
     * 프로젝트별 총 펀딩 금액, 리워드별 신청 수량 / 잔여 수량 / 서포터 및 서포터의 배송지
     */
    public void test01ViewFundingStatus() {
        header("01", "MAKER", "프로젝트별 펀딩 현황 보기");
        LoginUser maker = login(DataInitializer.MAKER1_ID, DataInitializer.MAKER_PW);

        List<Integer> projectIds;
        if (TEST01_PROJECT_ID != null) {
            projectIds = List.of(TEST01_PROJECT_ID);
        } else {
            projectIds = ctx.maker().getMyProjects(maker).getData().stream()
                    .filter(p -> p.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .map(ProjectSummary::getProjectId)
                    .collect(Collectors.toList());
        }

        for (int projectId : projectIds) {
            Response<FundingStatus> res = ctx.maker().getFundingStatus(maker, projectId);
            if (res.isSuccess()) {
                ResultView.printFundingStatus(res.getData());
            } else {
                ResultView.printResponse(res);
            }
        }
        checkSql("SELECT p.project_id, p.project_name, SUM(r.reward_price) AS total_amount, COUNT(*) AS supporters",
                "  FROM project p JOIN funding f ON f.project_id = p.project_id JOIN reward r ON r.reward_id = f.reward_id",
                "  WHERE p.user_id = 2 GROUP BY p.project_id;",
                "SELECT reward_id, project_id, reward_name, reward_amount, applied_count FROM reward WHERE project_id IN (1, 2, 3);",
                "SELECT f.project_id, f.reward_id, u.user_name, s.delivery_address FROM funding f",
                "  JOIN supporter s ON s.user_id = f.user_id JOIN `user` u ON u.user_id = f.user_id WHERE f.project_id IN (1, 2, 3);");
    }

    /** 검사 2. 프로젝트 기본 정보 등록 → approval_status = 'draft'(작성 중) 로 저장 */
    public void test02RegisterProject() {
        header("02", "MAKER", "프로젝트 기본 정보 등록");
        LoginUser maker = login(DataInitializer.MAKER1_ID, DataInitializer.MAKER_PW);

        LocalDate today = AppConfig.today();
        Project project = new Project(
                "캠핑용 LED 랜턴",                                                    // 프로젝트 이름
                "3단계 밝기 조절, 최대 48시간 사용. 방수 IPX4, 보조배터리 기능 포함.",   // 설명
                DataInitializer.CATEGORY_TECH,                                       // 카테고리 (1 = 테크·가전)
                800_000,                                                             // 목표 금액
                today,                                                               // 시작일
                today.plusDays(30));                                                 // 종료일

        Response<Integer> res = ctx.maker().registerProject(maker, project);
        ResultView.printResponse(res);

        ResultView.section("내 프로젝트 목록");
        ResultView.printManagedProjectList(ctx.maker().getMyProjects(maker).getData());
        checkSql("SELECT * FROM project ORDER BY project_id DESC LIMIT 1;");
    }

    /**
     * 검사 3. 리워드 등록 후 프로젝트 신청
     * 리워드가 없을 때 신청 → 실패 / 리워드 2개 등록(1개는 한정 수량) → 신청 → approval_status = 'waiting'
     */
    public void test03RegisterReward() {
        header("03", "MAKER", "리워드 등록 및 프로젝트 신청");
        LoginUser maker = login(DataInitializer.MAKER1_ID, DataInitializer.MAKER_PW);

        List<ProjectSummary> myProjects = ctx.maker().getMyProjects(maker).getData();
        ProjectSummary target = myProjects.stream()
                .filter(p -> TEST03_PROJECT_ID != null
                        ? p.getProjectId() == TEST03_PROJECT_ID
                        : p.getApprovalStatus() == ApprovalStatus.DRAFT)
                .findFirst()   // 목록이 최신순이므로 첫 번째 = 가장 최근
                .orElse(null);
        if (target == null) {
            System.out.println("리워드를 등록할 '작성 중' 프로젝트가 없습니다. test02RegisterProject() 를 먼저 실행하세요.");
            return;
        }
        int projectId = target.getProjectId();
        System.out.println("대상 프로젝트: [" + projectId + "] " + target.getProjectName());

        if (target.getRewardCount() == 0) {
            System.out.println("\n(1) 리워드 없이 신청 시도 — 실패해야 함");
            ResultView.printResponse(ctx.maker().submitProject(maker, projectId));
        }

        System.out.println("\n(2) 리워드 등록");
        LocalDateTime delivery = target.getEndDate().plusDays(14).atStartOfDay();   // 펀딩 종료 2주 뒤 제공
        ResultView.printResponse(ctx.maker().addReward(maker, new Reward(projectId,
                "[얼리버드] 캠핑 랜턴 1개", "정가 대비 20% 할인 (한정 5개)", 29_000, 5, delivery)));
        ResultView.printResponse(ctx.maker().addReward(maker, new Reward(projectId,
                "캠핑 랜턴 2개 세트", "랜턴 2개 + 전용 파우치", 55_000, null, delivery)));   // null = 무제한

        System.out.println("\n(3) 승인 신청");
        ResultView.printResponse(ctx.maker().submitProject(maker, projectId));

        ResultView.section("내 프로젝트 목록");
        ResultView.printManagedProjectList(ctx.maker().getMyProjects(maker).getData());
        checkSql("SELECT * FROM reward WHERE project_id = " + projectId + ";",
                "SELECT project_id, project_name, approval_status FROM project WHERE project_id = " + projectId + ";");
    }
}
