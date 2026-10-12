package crowdfunding.controller;

import crowdfunding.model.domain.Project;
import crowdfunding.model.domain.Reward;
import crowdfunding.model.dto.FundingStatus;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.model.enums.Role;
import crowdfunding.model.service.MakerService;
import crowdfunding.protocol.Response;

import java.util.List;

/** 메이커 요청 처리 — 메이커 권한 확인 후 MakerService 호출 */
public class MakerController extends BaseController {

    private final MakerService makerService;

    public MakerController(MakerService makerService) {
        this.makerService = makerService;
    }

    public Response<Integer> registerProject(LoginUser user, Project project) {
        return handle(() -> {
            requireRole(user, Role.MAKER);
            int projectId = makerService.registerProject(user, project);
            return Response.ok("프로젝트가 등록되었습니다. (" + projectId + "번, 작성 중) 리워드를 등록한 뒤 승인 신청하세요.", projectId);
        });
    }

    public Response<Integer> addReward(LoginUser user, Reward reward) {
        return handle(() -> {
            requireRole(user, Role.MAKER);
            int rewardId = makerService.addReward(user, reward);
            return Response.ok("리워드가 등록되었습니다. (" + rewardId + "번)", rewardId);
        });
    }

    public Response<Void> submitProject(LoginUser user, int projectId) {
        return handle(() -> {
            requireRole(user, Role.MAKER);
            makerService.submitProject(user, projectId);
            return Response.ok(projectId + "번 프로젝트를 승인 신청했습니다. (승인 대기)", null);
        });
    }

    public Response<List<ProjectSummary>> getMyProjects(LoginUser user) {
        return handle(() -> {
            requireRole(user, Role.MAKER);
            List<ProjectSummary> list = makerService.getMyProjects(user);
            return Response.ok("내 프로젝트 " + list.size() + "건", list);
        });
    }

    public Response<FundingStatus> getFundingStatus(LoginUser user, int projectId) {
        return handle(() -> {
            requireRole(user, Role.MAKER);
            return Response.ok(makerService.getFundingStatus(user, projectId));
        });
    }

    // [2차] public Response<Void> updateProject(LoginUser user, Project project) { ... }
}
