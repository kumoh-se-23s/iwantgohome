package crowdfunding.model.service;

import crowdfunding.config.AppConfig;
import crowdfunding.exception.BusinessException;
import crowdfunding.model.dao.ProjectMapper;
import crowdfunding.model.domain.Project;
import crowdfunding.model.enums.ApprovalStatus;
import crowdfunding.model.enums.Role;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * 관리자 기능
 *
 *   [검사 4] approveProject : 승인 대기 → 승인 (project_status = in_progress, 서포터에게 공개)
 *   [2차]    rejectProject  : 거절 + 사유 작성 → 12월 데모 때 추가
 *
 * [담당] A (검사 04)
 */
@RequiredArgsConstructor
public class AdminService {

    private final SqlSessionFactory sqlSessionFactory;   // Main 에서 생성자로 넣어 준다

    /** [검사 4] 메이커의 프로젝트 등록 신청 승인 */
    public void approveProject(int adminId, int projectId) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            try {
                ServiceGuard.requireUser(session, adminId, Role.ADMIN);
                Project project = ServiceGuard.requireProject(session, projectId);

                if (!ApprovalStatus.WAITING.matches(project.getApprovalStatus())) {
                    throw new BusinessException("승인 대기 중인 프로젝트만 승인할 수 있습니다. (현재: "
                            + ApprovalStatus.labelOf(project.getApprovalStatus()) + ")");
                }
                if (project.getEndDate().isBefore(AppConfig.today())) {
                    throw new BusinessException("펀딩 종료일이 이미 지난 프로젝트는 승인할 수 없습니다.");
                }

                // 승인 상태와 진행 상태를 한 문장으로 같이 바꾼다 (DB CHECK 제약)
                if (session.getMapper(ProjectMapper.class).approve(projectId) != 1) {
                    throw new BusinessException("승인에 실패했습니다. 다시 시도해 주세요.");
                }
                session.commit();
            } catch (RuntimeException e) {
                session.rollback();
                throw e;
            }
        }
    }

    // ---------------------------------------------------------------- 조회

    public Project findProject(int projectId) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            return session.getMapper(ProjectMapper.class).findById(projectId);
        }
    }

    /** 가장 최근에 승인 신청이 들어온 프로젝트 번호 (없으면 null) */
    public Integer findLatestWaitingProjectId() {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            return session.getMapper(ProjectMapper.class)
                    .findLatestProjectId(null, ApprovalStatus.WAITING.getValue());
        }
    }
}
