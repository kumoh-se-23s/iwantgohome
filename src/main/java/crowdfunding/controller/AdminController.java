package crowdfunding.controller;

import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.model.enums.Role;
import crowdfunding.model.service.AdminService;
import crowdfunding.protocol.Response;

import java.util.List;

/** 관리자 요청 처리 */
public class AdminController extends BaseController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    public Response<List<ProjectSummary>> getWaitingProjects(LoginUser user) {
        return handle(() -> {
            requireRole(user, Role.ADMIN);
            List<ProjectSummary> list = adminService.getWaitingProjects();
            return Response.ok("승인 대기 " + list.size() + "건", list);
        });
    }

    public Response<Void> approveProject(LoginUser user, int projectId) {
        return handle(() -> {
            requireRole(user, Role.ADMIN);
            adminService.approveProject(projectId);
            return Response.ok(projectId + "번 프로젝트를 승인했습니다. 이제 서포터에게 공개됩니다.", null);
        });
    }

    // [2차] rejectProject(user, projectId, reason), getFundingOverview(user, sort), getFundingStatus(user, projectId)
}
