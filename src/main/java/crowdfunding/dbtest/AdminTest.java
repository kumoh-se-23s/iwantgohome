package crowdfunding.dbtest;

import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.protocol.Response;
import crowdfunding.view.ResultView;

import java.util.Comparator;
import java.util.List;

/** 1차 검사 — 관리자 (검사 4) */
public class AdminTest extends DbTestSupport {

    // ===== 검사 시 수정 가능한 값 =====
    /** 승인할 프로젝트 번호 (null = 승인 대기 중 번호가 가장 큰 프로젝트 = 검사 3에서 신청한 것) */
    private static final Integer TEST04_PROJECT_ID = null;

    /** 검사 4. 메이커의 프로젝트 등록 신청 승인 → approved + in_progress (서포터에게 공개) */
    public void test04ApproveProject() {
        header("04", "ADMIN", "프로젝트 등록 승인");
        LoginUser admin = login(DataInitializer.ADMIN_ID, DataInitializer.ADMIN_PW);

        Response<List<ProjectSummary>> waiting = ctx.admin().getWaitingProjects(admin);
        ResultView.section("승인 대기 목록");
        ResultView.printManagedProjectList(waiting.getData());

        Integer projectId = TEST04_PROJECT_ID;
        if (projectId == null) {
            projectId = waiting.getData().stream()
                    .map(ProjectSummary::getProjectId)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        }
        if (projectId == null) {
            System.out.println("승인 대기 중인 프로젝트가 없습니다.");
            return;
        }

        System.out.println();
        ResultView.printResponse(ctx.admin().approveProject(admin, projectId));

        ResultView.section("승인 후 승인 대기 목록");
        ResultView.printManagedProjectList(ctx.admin().getWaitingProjects(admin).getData());
        checkSql("SELECT project_id, project_name, approval_status, project_status FROM project WHERE project_id = " + projectId + ";");
    }
}
