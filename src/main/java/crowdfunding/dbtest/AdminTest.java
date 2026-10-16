package crowdfunding.dbtest;

import crowdfunding.model.service.AdminService;
import crowdfunding.view.ResultView;
import lombok.RequiredArgsConstructor;

/**
 * 관리자 검사 항목
 *   [검사 4] test04ApproveProject : 메이커의 프로젝트 등록 신청 승인
 *
 * [담당] A (검사 04)
 */
@RequiredArgsConstructor
public class AdminTest {

    // ============================================================================
    //  검사 때 바꿔도 되는 값 (번호는 initDB 기준)
    // ============================================================================
    /** 관리자 계정: 1 = admin01 */
    private static final int ADMIN_ID = 1;

    /**
     * 승인할 프로젝트. 0 이면 가장 최근에 승인 신청된 프로젝트(= test03 에서 신청한 것)를 자동 선택.
     * initDB 직후라면 6번(비건 단백질 쿠키)이 승인 대기 상태다.
     */
    private static final int TEST04_TARGET_PROJECT_ID = 0;
    // ============================================================================

    // 사용할 Service 들 - Main 에서 만들어서 생성자로 넣어 준다 (@RequiredArgsConstructor)
    private final AdminService adminService;

    /** [검사 4] 승인 → approval_status = 'approved', project_status = 'in_progress' (서포터에게 공개) */
    public void test04ApproveProject() {
        ResultView.title("[검사 4] ADMIN - 메이커의 프로젝트 등록 신청 승인 (관리자 user " + ADMIN_ID + ")");

        int projectId = TEST04_TARGET_PROJECT_ID;
        if (projectId == 0) {
            Integer latest = adminService.findLatestWaitingProjectId();
            if (latest == null) {
                ResultView.fail("승인 대기 중인 프로젝트가 없습니다. TEST04_TARGET_PROJECT_ID 에 번호를 넣으세요.");
                return;
            }
            projectId = latest;
            ResultView.info("대상: 프로젝트 " + projectId + "번 (가장 최근 승인 신청 프로젝트 자동 선택)");
        }

        final int target = projectId;
        ResultView.project("승인 전", adminService.findProject(target));
        TestSupport.attempt("프로젝트 " + target + "번 승인", () -> adminService.approveProject(ADMIN_ID, target));
        ResultView.project("승인 후", adminService.findProject(target));

        ResultView.checkSql("SELECT project_id, project_name, approval_status, project_status FROM project "
                + "WHERE project_id = " + target + ";");
    }
}
