package crowdfunding.controller;

import crowdfunding.model.domain.Category;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProjectDetail;
import crowdfunding.model.dto.ProjectSearchCondition;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.model.dto.ReviewView;
import crowdfunding.model.service.ProjectQueryService;
import crowdfunding.protocol.Response;

import java.util.List;

/** 공통 조회 (로그인한 모든 사용자) — 프로젝트 리스트, 리워드 등 상세 정보, 리뷰 */
public class ProjectController extends BaseController {

    private final ProjectQueryService queryService;

    public ProjectController(ProjectQueryService queryService) {
        this.queryService = queryService;
    }

    public Response<List<Category>> getCategories(LoginUser user) {
        return handle(() -> {
            requireRole(user);
            return Response.ok(queryService.getCategories());
        });
    }

    /** 프로젝트 보기(기본) / 필터링 / 정렬 — 조건이 null 이면 전체 목록 */
    public Response<List<ProjectSummary>> searchProjects(LoginUser user, ProjectSearchCondition condition) {
        return handle(() -> {
            requireRole(user);
            List<ProjectSummary> list = queryService.searchProjects(condition);
            return Response.ok("프로젝트 " + list.size() + "건", list);
        });
    }

    public Response<ProjectDetail> getProjectDetail(LoginUser user, int projectId) {
        return handle(() -> {
            requireRole(user);
            return Response.ok(queryService.getProjectDetail(user, projectId));
        });
    }

    public Response<List<ReviewView>> getProjectReviews(LoginUser user, int projectId) {
        return handle(() -> {
            requireRole(user);
            List<ReviewView> reviews = queryService.getProjectReviews(projectId);
            return Response.ok("리뷰 " + reviews.size() + "건", reviews);
        });
    }
}
