package crowdfunding.model.service;

import crowdfunding.config.AppConfig;
import crowdfunding.exception.BusinessException;
import crowdfunding.model.dao.CategoryMapper;
import crowdfunding.model.dao.ProjectMapper;
import crowdfunding.model.dao.RewardMapper;
import crowdfunding.model.domain.Project;
import crowdfunding.model.domain.Reward;
import crowdfunding.model.enums.ApprovalStatus;
import crowdfunding.model.enums.Role;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.time.LocalDate;
import java.util.List;

/**
 * 메이커의 프로젝트 등록 → 리워드 등록 → 승인 신청
 *
 *   [검사 2] registerProject : 기본 정보 등록 → 'draft'(작성 중)
 *   [검사 3] addReward       : 리워드 등록 (작성 중 / 거절 상태에서만)
 *            submitProject   : 리워드 등록이 끝난 뒤 승인 신청 → 'waiting'(승인 대기)
 *
 * 쓰기 메소드는 모두 같은 모양이다.
 *   try (세션 열기) { try { 규칙 검사 → SQL 실행 → commit } catch { rollback 후 예외 다시 던지기 } }
 *
 * [담당] A (검사 02·03)
 */
@RequiredArgsConstructor
public class MakerProjectService {

    private final SqlSessionFactory sqlSessionFactory;   // Main 에서 생성자로 넣어 준다

    /**
     * [검사 2] 프로젝트 기본 정보 등록
     * @return 새로 만들어진 project_id
     */
    public int registerProject(int makerId, Project project) {
        LocalDate today = AppConfig.today();
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            try {
                ServiceGuard.requireUser(session, makerId, Role.MAKER);
                validateBasicInfo(session, project, today);

                project.setUserId(makerId);
                session.getMapper(ProjectMapper.class).insertBasicInfo(project);   // projectId 가 채워진다
                session.commit();
                return project.getProjectId();
            } catch (RuntimeException e) {
                session.rollback();
                throw e;
            }
        }
    }

    /** 기본 정보 입력 검증 (요구사항: 기본 정보, 카테고리, 목표 금액, 펀딩 기간) */
    private void validateBasicInfo(SqlSession session, Project p, LocalDate today) {
        if (p == null) {
            throw new BusinessException("프로젝트 정보가 없습니다.");
        }
        if (ServiceGuard.isBlank(p.getProjectName())) {
            throw new BusinessException("프로젝트 이름을 입력해 주세요.");
        }
        if (p.getProjectName().length() > 255) {
            throw new BusinessException("프로젝트 이름은 255자 이하로 입력해 주세요.");
        }
        if (p.getTargetAmount() <= 0) {
            throw new BusinessException("목표 금액은 0원보다 커야 합니다.");
        }
        if (p.getStartDate() == null || p.getEndDate() == null) {
            throw new BusinessException("펀딩 시작일과 종료일을 입력해 주세요.");
        }
        if (p.getStartDate().isBefore(today)) {
            throw new BusinessException("펀딩 시작일은 오늘(" + today + ") 이후여야 합니다.");
        }
        if (p.getEndDate().isBefore(p.getStartDate())) {
            throw new BusinessException("펀딩 종료일은 시작일 이후여야 합니다.");
        }
        if (!session.getMapper(CategoryMapper.class).exists(p.getCategoryId())) {
            throw new BusinessException("존재하지 않는 카테고리입니다. (category_id=" + p.getCategoryId() + ")");
        }
    }

    /**
     * [검사 3] 리워드 등록. 승인 신청 전(작성 중) 또는 거절된 프로젝트에만 추가할 수 있다.
     * (승인 뒤에 리워드나 가격이 바뀌면 이미 후원한 금액과 맞지 않게 되므로)
     * @return 새로 만들어진 reward_id
     */
    public int addReward(int makerId, int projectId, Reward reward) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            try {
                ServiceGuard.requireUser(session, makerId, Role.MAKER);
                Project project = ServiceGuard.requireOwnProject(session, makerId, projectId);
                requireEditable(project);
                validateReward(reward, project);

                reward.setProjectId(projectId);
                reward.setAppliedCount(0);
                session.getMapper(RewardMapper.class).insert(reward);   // rewardId 가 채워진다
                session.commit();
                return reward.getRewardId();
            } catch (RuntimeException e) {
                session.rollback();
                throw e;
            }
        }
    }

    /** 리워드 입력 검증 (요구사항: 명칭, 제공 내용, 후원 금액, 제공 가능 수량, 제공 시기) */
    private void validateReward(Reward r, Project project) {
        if (r == null) {
            throw new BusinessException("리워드 정보가 없습니다.");
        }
        if (ServiceGuard.isBlank(r.getRewardName())) {
            throw new BusinessException("리워드 이름을 입력해 주세요.");
        }
        if (r.getRewardPrice() <= 0) {
            throw new BusinessException("리워드 금액은 0원보다 커야 합니다.");
        }
        if (r.getRewardAmount() != null && r.getRewardAmount() < 1) {
            throw new BusinessException("제공 가능 수량은 1개 이상이어야 합니다. (무제한이면 비워 두세요)");
        }
        if (r.getRewardDeliveryDate() != null
                && r.getRewardDeliveryDate().toLocalDate().isBefore(project.getEndDate())) {
            throw new BusinessException("리워드 제공 시기는 펀딩 종료일(" + project.getEndDate() + ") 이후여야 합니다.");
        }
    }

    /**
     * [검사 3] 승인 신청 : 리워드 등록이 끝난 프로젝트를 관리자 승인 대기로 보낸다.
     * 조건 - 리워드 2개 이상 (구현 범위 2.1), 수량 제한 리워드 1개 이상 (2.2)
     */
    public void submitProject(int makerId, int projectId) {
        LocalDate today = AppConfig.today();
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            try {
                ServiceGuard.requireUser(session, makerId, Role.MAKER);
                Project project = ServiceGuard.requireOwnProject(session, makerId, projectId);
                requireEditable(project);

                if (project.getEndDate().isBefore(today)) {
                    throw new BusinessException("펀딩 종료일이 이미 지났습니다. 기간을 수정한 뒤 신청해 주세요.");
                }

                RewardMapper rewardMapper = session.getMapper(RewardMapper.class);
                int rewardCount = rewardMapper.countByProject(projectId);
                if (rewardCount < AppConfig.MIN_REWARDS_TO_SUBMIT) {
                    throw new BusinessException("리워드를 " + AppConfig.MIN_REWARDS_TO_SUBMIT
                            + "개 이상 등록해야 신청할 수 있습니다. (현재 " + rewardCount + "개)");
                }
                if (AppConfig.REQUIRE_LIMITED_REWARD && rewardMapper.countLimitedByProject(projectId) == 0) {
                    throw new BusinessException("제공 가능 수량이 제한된 리워드가 1개 이상 있어야 합니다.");
                }

                if (session.getMapper(ProjectMapper.class).submit(projectId) != 1) {
                    throw new BusinessException("승인 신청에 실패했습니다. 프로젝트 상태를 확인해 주세요.");
                }
                session.commit();
            } catch (RuntimeException e) {
                session.rollback();
                throw e;
            }
        }
    }

    /** 리워드 추가 · 승인 신청은 작성 중(draft) 이거나 거절(reject)된 프로젝트에서만 */
    private void requireEditable(Project project) {
        String status = project.getApprovalStatus();
        if (!ApprovalStatus.DRAFT.matches(status) && !ApprovalStatus.REJECT.matches(status)) {
            throw new BusinessException("작성 중이거나 거절된 프로젝트만 수정·신청할 수 있습니다. (현재: "
                    + ApprovalStatus.labelOf(status) + ")");
        }
    }

    // ---------------------------------------------------------------- 조회

    public Project findProject(int projectId) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            return session.getMapper(ProjectMapper.class).findById(projectId);
        }
    }

    public List<Reward> findRewards(int projectId) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            return session.getMapper(RewardMapper.class).findByProject(projectId);
        }
    }

    /** 이 메이커의 프로젝트 중 해당 승인 상태인 가장 최근 프로젝트 번호 (없으면 null) */
    public Integer findLatestProjectId(int makerId, ApprovalStatus status) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            return session.getMapper(ProjectMapper.class).findLatestProjectId(makerId, status.getValue());
        }
    }
}
