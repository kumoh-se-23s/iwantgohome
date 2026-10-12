package crowdfunding.view;

import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.network.client.ServerConnection;
import crowdfunding.protocol.Command;

import java.util.List;

/** [2차] 관리자 메뉴 */
public class AdminMenu extends BaseMenu {

    public AdminMenu(ServerConnection conn, ConsoleInput in, LoginUser user) {
        super(conn, in, user);
    }

    @Override
    protected String title() {
        return "관리자 메뉴";
    }

    @Override
    protected void printMenu() {
        System.out.println("  1. 승인 대기 프로젝트 목록");
        System.out.println("  2. 프로젝트 승인");
        System.out.println("  3. 전체 프로젝트 보기");
        System.out.println("  4. 프로젝트 상세 보기");
        // [2차 추가] 5. 프로젝트 거절(사유 작성)   6. 프로젝트별 펀딩 현황(정렬)
    }

    @Override
    protected boolean handle(int choice) {
        switch (choice) {
            case 1: showWaitingProjects(); return true;
            case 2: approveProject(); return true;
            case 3: showProjects(null); return true;
            case 4: showProjectDetail(null); return true;
            default: return false;
        }
    }

    private List<ProjectSummary> showWaitingProjects() {
        List<ProjectSummary> list = request(Command.ADMIN_WAITING_PROJECTS, null);
        if (list != null) {
            ResultView.title("승인 대기 프로젝트");
            ResultView.printManagedProjectList(list);
        }
        return list;
    }

    private void approveProject() {
        List<ProjectSummary> waiting = showWaitingProjects();
        if (waiting == null || waiting.isEmpty()) {
            return;
        }
        Integer projectId = in.optionalNumber("승인할 프로젝트 번호(엔터=취소): ");
        if (projectId == null) {
            return;
        }
        showProjectDetail(projectId);   // 승인 전에 리워드까지 확인
        if (in.confirm(projectId + "번 프로젝트를 승인할까요?")) {
            requestAndPrint(Command.ADMIN_APPROVE_PROJECT, projectId);
        }
    }
}
