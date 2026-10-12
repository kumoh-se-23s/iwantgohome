package crowdfunding.model.service;

import crowdfunding.config.AppConfig;
import crowdfunding.config.TransactionManager;
import crowdfunding.model.dao.ProjectMapper;
import crowdfunding.model.domain.Project;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.model.enums.ApprovalStatus;

import java.util.List;

import static crowdfunding.model.service.Validator.require;

/**
 * 관리자 기능
 * <pre>
 *  1차: 승인 대기 목록, 프로젝트 승인
 *  2차: 거절 + 사유 작성(rejectProject), 전체 프로젝트 펀딩 현황 + 정렬(getFundingOverview)
 * </pre>
 */
public class AdminService {

    /** 승인 대기 중인 프로젝트 (먼저 신청한 순) */
    public List<ProjectSummary> getWaitingProjects() {
        return TransactionManager.execute(session ->
                session.getMapper(ProjectMapper.class).findByApprovalStatus(ApprovalStatus.WAITING, AppConfig.today()));
    }

    /** 승인 → 서포터에게 공개되고 펀딩 진행 중(in_progress)이 된다 */
    public void approveProject(int projectId) {
        TransactionManager.run(session -> {
            ProjectMapper mapper = session.getMapper(ProjectMapper.class);
            Project project = mapper.findById(projectId);
            require(project != null, projectId + "번 프로젝트가 존재하지 않습니다.");
            require(project.getApprovalStatus() == ApprovalStatus.WAITING,
                    "승인 대기 중인 프로젝트만 승인할 수 있습니다. (현재: " + project.getApprovalStatus().getLabel() + ")");
            require(!project.getEndDate().isBefore(AppConfig.today()),
                    "종료일이 이미 지난 프로젝트는 승인할 수 없습니다.");

            // WHERE approval_status = 'waiting' 조건 → 관리자 두 명이 동시에 눌러도 한 번만 처리
            int changed = mapper.approve(projectId);
            require(changed == 1, "이미 처리된 프로젝트입니다.");
        });
    }

    // [2차] 거절 및 사유 작성
    // public void rejectProject(int projectId, String reason) {
    //     reason 필수 → WAITING 확인 → projectMapper.reject(projectId, reason)
    //     (CHECK ck_project_reject: reject 상태면 reject_reason NOT NULL)
    // }

    // [2차] 프로젝트별 펀딩 현황 (기본 최신순 + 총 펀딩 금액/달성률/서포터 수 오름·내림차순)
    // public List<ProjectSummary> getFundingOverview(AdminSort sort, boolean ascending) {
    //     ProjectMapper.xml 에 <choose> 로 ORDER BY 를 고르는 쿼리 추가
    //     상세는 MakerService.buildFundingStatus(session, projectId) 재사용
    // }
}
