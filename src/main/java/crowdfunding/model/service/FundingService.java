package crowdfunding.model.service;

import crowdfunding.config.AppConfig;
import crowdfunding.exception.BusinessException;
import crowdfunding.exception.DbErrors;
import crowdfunding.model.dao.FundingMapper;
import crowdfunding.model.dao.RewardMapper;
import crowdfunding.model.dao.SupporterMapper;
import crowdfunding.model.domain.Funding;
import crowdfunding.model.domain.Project;
import crowdfunding.model.domain.Reward;
import crowdfunding.model.enums.ApprovalStatus;
import crowdfunding.model.enums.ProjectStatus;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.time.LocalDate;

/**
 * 서포터의 리워드 신청 (후원)
 *
 *   [검사 8]  applyReward : 리워드 수량이 줄고, 후원 기록이 남고, 메이커가 확인할 수 있다
 *   [검사 10] applyReward : 수량이 남았으면 성공(기록 O, 수량 변경 O)
 *                           수량이 없으면 실패(기록 X, 수량 변경 X)
 *
 * ★ 동시성 제어 방법 (시연 때 설명할 부분)
 *   리워드 신청 전체가 하나의 트랜잭션이다.
 *   1) SELECT ... FOR UPDATE 로 리워드 행을 잠근다
 *      → 같은 리워드를 동시에 신청한 다른 트랜잭션은 여기서 기다렸다가, 앞사람이 끝난 뒤의 줄어든 수량을 읽는다
 *   2) 남은 수량이 없으면 예외 → 롤백 → 후원 기록도, 수량 변경도 남지 않는다
 *   3) 조건부 UPDATE (applied_count < reward_amount 일 때만 +1) → 0행이면 실패 (이중 안전장치)
 *   4) funding INSERT → PK(user_id, project_id) 가 같은 서포터의 중복 후원을 막는다
 *   5) commit 해야 3)과 4)가 함께 반영된다. 중간에 실패하면 둘 다 취소된다.
 *   + 최종 안전장치: 테이블 CHECK 제약 (applied_count ≤ reward_amount)
 *
 * [담당] C (검사 08·10)
 */
@RequiredArgsConstructor
public class FundingService {

    private final SqlSessionFactory sqlSessionFactory;   // Main 에서 생성자로 넣어 준다

    /**
     * 리워드 신청. 실패하면 BusinessException(이유)을 던지고 DB 는 그대로 남는다.
     * @param userId    신청하는 서포터
     * @param projectId 후원할 프로젝트
     * @param rewardId  그 프로젝트의 리워드
     */
    public void applyReward(int userId, int projectId, int rewardId) {
        LocalDate today = AppConfig.today();
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            try {
                RewardMapper rewardMapper = session.getMapper(RewardMapper.class);
                FundingMapper fundingMapper = session.getMapper(FundingMapper.class);

                // 0) 서포터만 후원할 수 있다 (funding.user_id → supporter FK)
                if (!session.getMapper(SupporterMapper.class).exists(userId)) {
                    throw new BusinessException("서포터만 후원할 수 있습니다. (user_id=" + userId + ")");
                }

                // 1) 후원할 수 있는 프로젝트인지: 승인됨 + 진행 중 + 오늘이 펀딩 기간 안
                Project project = ServiceGuard.requireProject(session, projectId);
                if (!ApprovalStatus.APPROVED.matches(project.getApprovalStatus())
                        || !ProjectStatus.IN_PROGRESS.matches(project.getProjectStatus())) {
                    throw new BusinessException("펀딩 중인 프로젝트가 아닙니다. (현재: "
                            + ApprovalStatus.labelOf(project.getApprovalStatus()) + " / "
                            + ProjectStatus.labelOf(project.getProjectStatus()) + ")");
                }
                if (today.isBefore(project.getStartDate()) || today.isAfter(project.getEndDate())) {
                    throw new BusinessException("펀딩 기간이 아닙니다. (" + project.getStartDate()
                            + " ~ " + project.getEndDate() + ")");
                }

                // 2) 하나의 프로젝트에는 하나의 리워드(후원)만 신청 가능
                if (fundingMapper.exists(userId, projectId)) {
                    throw new BusinessException("이미 후원한 프로젝트입니다. (프로젝트당 리워드 1개만 신청 가능)");
                }

                // 3) ★ 리워드 행 잠금 → 이 트랜잭션이 끝날 때까지 같은 리워드 신청은 줄을 선다
                Reward reward = rewardMapper.findByIdForUpdate(rewardId);
                if (reward == null || reward.getProjectId() != projectId) {
                    throw new BusinessException("이 프로젝트의 리워드가 아닙니다. (reward_id=" + rewardId + ")");
                }
                if (!reward.hasStock()) {
                    throw new BusinessException("리워드 잔여 수량이 부족합니다. (수량 " + reward.getRewardAmount()
                            + "개 중 " + reward.getAppliedCount() + "개 신청 완료)");
                }

                // 4) 수량 차감 (조건부 UPDATE: 남아 있을 때만 +1)
                if (rewardMapper.increaseAppliedCount(rewardId, projectId) != 1) {
                    throw new BusinessException("리워드 잔여 수량이 부족합니다.");
                }

                // 5) 후원 기록 남기기
                try {
                    fundingMapper.insert(userId, projectId, rewardId);
                } catch (PersistenceException e) {
                    if (DbErrors.isDuplicateKey(e)) {   // 같은 서포터가 동시에 두 번 누른 경우
                        throw new BusinessException("이미 후원한 프로젝트입니다. (프로젝트당 리워드 1개만 신청 가능)");
                    }
                    throw e;
                }

                session.commit();   // 여기까지 와야 수량 변경 + 후원 기록이 함께 반영된다
            } catch (RuntimeException e) {
                session.rollback(); // 하나라도 실패하면 둘 다 취소
                throw e;
            }
        }
    }

    // ---------------------------------------------------------------- 조회 (검사 결과 확인용)

    public Reward findReward(int rewardId) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            return session.getMapper(RewardMapper.class).findById(rewardId);
        }
    }

    public Funding findFunding(int userId, int projectId) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            return session.getMapper(FundingMapper.class).find(userId, projectId);
        }
    }

    public int countFundingsOfReward(int rewardId) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            return session.getMapper(FundingMapper.class).countByReward(rewardId);
        }
    }
}
