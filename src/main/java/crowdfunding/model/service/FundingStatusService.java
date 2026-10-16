package crowdfunding.model.service;

import crowdfunding.exception.BusinessException;
import crowdfunding.model.dao.ProjectMapper;
import crowdfunding.model.dto.FundingStatus;
import crowdfunding.model.enums.Role;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.List;

/**
 * 메이커의 프로젝트별 펀딩 현황
 *
 *   [검사 1] getFundingStatus(makerId)            : 내 승인된 프로젝트 전부
 *   [검사 8] getFundingStatus(makerId, projectId) : 리워드 신청 뒤 "내 프로젝트에 누가 펀딩했는지" 확인
 *
 * 보여 주는 것: 프로젝트별 총 펀딩 금액 · 달성률,
 *              리워드별 신청 수량 / 잔여 수량, 신청한 서포터와 배송지
 *
 * [담당] D (검사 01)
 */
@RequiredArgsConstructor
public class FundingStatusService {

    private final SqlSessionFactory sqlSessionFactory;   // Main 에서 생성자로 넣어 준다
    private final ProjectCloseService closeService;

    /** [검사 1] 메이커의 승인된 프로젝트 전체의 펀딩 현황 (최신 프로젝트 먼저) */
    public List<FundingStatus> getFundingStatus(int makerId) {
        closeService.closeExpiredProjects();
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            ServiceGuard.requireUser(session, makerId, Role.MAKER);
            return session.getMapper(ProjectMapper.class).findFundingStatus(makerId, null);
        }
    }

    /** 메이커의 프로젝트 한 개의 펀딩 현황 (본인 프로젝트만 볼 수 있음) */
    public FundingStatus getFundingStatus(int makerId, int projectId) {
        closeService.closeExpiredProjects();
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            ServiceGuard.requireUser(session, makerId, Role.MAKER);
            ServiceGuard.requireOwnProject(session, makerId, projectId);
            List<FundingStatus> list = session.getMapper(ProjectMapper.class).findFundingStatus(makerId, projectId);
            if (list.isEmpty()) {
                throw new BusinessException("승인된 프로젝트만 펀딩 현황을 볼 수 있습니다. (project_id=" + projectId + ")");
            }
            return list.get(0);
        }
    }

    /** 프로젝트를 만든 메이커의 user_id */
    public int findMakerIdOf(int projectId) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            return ServiceGuard.requireProject(session, projectId).getUserId();
        }
    }
}
