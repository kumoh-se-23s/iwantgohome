package crowdfunding.model.service;

import crowdfunding.config.AppConfig;
import crowdfunding.config.TransactionManager;
import crowdfunding.model.dao.CategoryMapper;
import crowdfunding.model.dao.FundingMapper;
import crowdfunding.model.dao.LikeMapper;
import crowdfunding.model.dao.ProjectMapper;
import crowdfunding.model.dao.ReviewMapper;
import crowdfunding.model.dao.RewardMapper;
import crowdfunding.model.domain.Category;
import crowdfunding.model.domain.Funding;
import crowdfunding.model.domain.Project;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProjectDetail;
import crowdfunding.model.dto.ProjectSearchCondition;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.model.dto.ReviewView;
import crowdfunding.model.enums.ApprovalStatus;
import crowdfunding.model.enums.FundingState;
import crowdfunding.model.enums.ProjectSort;
import crowdfunding.model.enums.Role;

import java.util.List;

import static crowdfunding.model.service.Validator.require;

/** 공통 조회 기능 — 프로젝트 목록/검색, 상세 정보(리워드), 리뷰, 카테고리 */
public class ProjectQueryService {

    public List<Category> getCategories() {
        return TransactionManager.execute(session -> session.getMapper(CategoryMapper.class).findAll());
    }

    /**
     * 프로젝트 목록/검색 (승인된 프로젝트만).
     * 조건이 없으면 전체 목록(최신순) = "프로젝트 보기(기본)"
     */
    public List<ProjectSummary> searchProjects(ProjectSearchCondition input) {
        ProjectSearchCondition condition = new ProjectSearchCondition();
        if (input != null) {
            condition.setKeyword(Validator.isBlank(input.getKeyword()) ? null : input.getKeyword().trim());
            condition.setCategoryId(input.getCategoryId());
            condition.setState(input.getState() == null ? FundingState.ALL : input.getState());
            condition.setSort(input.getSort() == null ? ProjectSort.LATEST : input.getSort());
        }
        condition.setToday(AppConfig.today());   // 기준 날짜는 서버가 정함

        return TransactionManager.execute(session -> session.getMapper(ProjectMapper.class).searchApproved(condition));
    }

    /**
     * 프로젝트 상세 보기 (리워드 목록 + 잔여 수량).
     * 승인 전 프로젝트는 그 프로젝트의 메이커와 관리자만 볼 수 있다.
     */
    public ProjectDetail getProjectDetail(LoginUser viewer, int projectId) {
        return TransactionManager.execute(session -> {
            Project project = session.getMapper(ProjectMapper.class).findById(projectId);
            require(project != null, projectId + "번 프로젝트가 존재하지 않습니다.");
            boolean visible = project.getApprovalStatus() == ApprovalStatus.APPROVED
                    || viewer.getRole() == Role.ADMIN
                    || project.getMakerId() == viewer.getUserId();
            require(visible, "아직 공개되지 않은 프로젝트입니다.");

            ProjectDetail detail = new ProjectDetail();
            detail.setSummary(session.getMapper(ProjectMapper.class).findSummaryById(projectId, AppConfig.today()));
            detail.setProjectDescription(project.getProjectDescription());
            detail.setRewards(session.getMapper(RewardMapper.class).findViewsByProject(projectId));

            if (viewer.getRole() == Role.SUPPORTER) {
                Funding mine = session.getMapper(FundingMapper.class).findByUserAndProject(viewer.getUserId(), projectId);
                detail.setMyRewardId(mine == null ? null : mine.getRewardId());
                detail.setLikedByMe(session.getMapper(LikeMapper.class).existsLike(projectId, viewer.getUserId()));
            }
            return detail;
        });
    }

    /** 프로젝트 리뷰 (리워드명 / 별점 / 리뷰, 최신순) — 2차 메이커 '프로젝트 리뷰 조회'에서도 사용 */
    public List<ReviewView> getProjectReviews(int projectId) {
        return TransactionManager.execute(session -> session.getMapper(ReviewMapper.class).findViewsByProject(projectId));
    }
}
