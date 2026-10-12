package crowdfunding.model.service;

import crowdfunding.config.AppConfig;
import crowdfunding.config.TransactionManager;
import crowdfunding.model.dao.CategoryMapper;
import crowdfunding.model.dao.ProjectMapper;
import crowdfunding.model.dao.RewardMapper;
import crowdfunding.model.domain.Project;
import crowdfunding.model.domain.Reward;
import crowdfunding.model.dto.FundingStatus;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.model.dto.RewardFundingStatus;
import crowdfunding.model.enums.ApprovalStatus;
import org.apache.ibatis.session.SqlSession;

import java.time.LocalDate;
import java.util.List;

import static crowdfunding.model.service.Validator.require;

/**
 * 메이커 기능
 * <pre>
 *  1차: 프로젝트 기본 정보 등록 → 리워드 등록 → 승인 신청, 내 프로젝트 목록, 프로젝트별 펀딩 현황
 *  2차: 거절된 프로젝트 수정(updateProject) — 재신청은 submitProject 가 이미 처리함
 * </pre>
 */
public class MakerService {

    /** 프로젝트 기본 정보 등록 → '작성 중(draft)' 상태로 저장. 생성된 프로젝트 번호 반환 */
    public int registerProject(LoginUser maker, Project input) {
        Project project = validateProjectInfo(input);

        return TransactionManager.execute(session -> {
            require(session.getMapper(CategoryMapper.class).findById(project.getCategoryId()) != null,
                    "존재하지 않는 카테고리입니다.");

            project.setMakerId(maker.getUserId());           // 클라이언트가 보낸 값이 아니라 로그인 정보 사용
            project.setApprovalStatus(ApprovalStatus.DRAFT); // 리워드 등록 후 신청해야 승인 대기로 바뀜
            project.setProjectStatus(null);
            project.setRejectReason(null);
            project.setCreatedAt(null);                       // DB 의 현재 시각
            session.getMapper(ProjectMapper.class).insertProject(project);
            return project.getProjectId();
        });
    }

    /** 리워드 등록 — 작성 중(또는 거절된) 본인 프로젝트에만 가능. 생성된 리워드 번호 반환 */
    public int addReward(LoginUser maker, Reward input) {
        require(input != null && input.getProjectId() != null, "리워드를 등록할 프로젝트를 선택하세요.");
        Reward reward = new Reward(input.getProjectId(),
                Validator.requireText(input.getRewardName(), "리워드 이름", 1, 255),
                Validator.optionalText(input.getRewardDescription(), "제공 내용", 255),
                input.getRewardPrice(), input.getRewardAmount(), input.getRewardDeliveryDate());
        require(reward.getRewardPrice() > 0, "후원 금액은 1원 이상이어야 합니다.");
        require(reward.getRewardAmount() == null || reward.getRewardAmount() >= 1,
                "제공 가능 수량은 1개 이상이어야 합니다. (무제한이면 비워 두세요)");
        validateImage(input, reward);

        return TransactionManager.execute(session -> {
            Project project = findOwnedProject(session, maker, reward.getProjectId());
            require(project.getApprovalStatus().isEditable(),
                    "작성 중이거나 거절된 프로젝트에만 리워드를 등록할 수 있습니다. (현재: "
                            + project.getApprovalStatus().getLabel() + ")");
            if (reward.getRewardDeliveryDate() != null) {
                require(!reward.getRewardDeliveryDate().toLocalDate().isBefore(project.getEndDate()),
                        "리워드 제공 시기는 펀딩 종료일(" + project.getEndDate() + ") 이후여야 합니다.");
            }
            reward.setAppliedCount(0);
            session.getMapper(RewardMapper.class).insertReward(reward);
            return reward.getRewardId();
        });
    }

    /**
     * 승인 신청 — 작성 중(draft) → 승인 대기(waiting).
     * 거절(reject)된 프로젝트의 재신청도 같은 메소드로 처리한다. (거절 사유는 지워짐)
     */
    public void submitProject(LoginUser maker, int projectId) {
        TransactionManager.run(session -> {
            Project project = findOwnedProject(session, maker, projectId);
            require(project.getApprovalStatus().isEditable(),
                    "작성 중이거나 거절된 프로젝트만 신청할 수 있습니다. (현재: "
                            + project.getApprovalStatus().getLabel() + ")");
            int rewardCount = session.getMapper(RewardMapper.class).countByProject(projectId);
            require(rewardCount >= AppConfig.MIN_REWARDS_TO_SUBMIT,
                    "리워드를 " + AppConfig.MIN_REWARDS_TO_SUBMIT + "개 이상 등록한 뒤 신청할 수 있습니다.");
            require(!project.getEndDate().isBefore(AppConfig.today()),
                    "종료일이 지난 프로젝트는 신청할 수 없습니다. 펀딩 기간을 수정하세요.");

            int changed = session.getMapper(ProjectMapper.class).submit(projectId);
            require(changed == 1, "프로젝트 상태가 변경되어 신청하지 못했습니다. 다시 확인하세요.");
        });
    }

    /** 내 프로젝트 목록 (승인 상태 / 진행 상태 / 금액 포함) */
    public List<ProjectSummary> getMyProjects(LoginUser maker) {
        return TransactionManager.execute(session ->
                session.getMapper(ProjectMapper.class).findByMaker(maker.getUserId(), AppConfig.today()));
    }

    /** 프로젝트별 펀딩 현황 — 본인 프로젝트만 */
    public FundingStatus getFundingStatus(LoginUser maker, int projectId) {
        return TransactionManager.execute(session -> {
            findOwnedProject(session, maker, projectId);
            return buildFundingStatus(session, projectId);
        });
    }

    // [2차] 거절된 프로젝트 수정
    // public void updateProject(LoginUser maker, int projectId, Project input) {
    //     validateProjectInfo(input) → findOwnedProject → isEditable() 확인 → projectMapper.updateProjectInfo(...)
    //     수정 후 submitProject(maker, projectId) 로 재신청
    // }

    // ------------------------------------------------------------------

    /** 펀딩 현황 조립 (2차 관리자 '프로젝트별 펀딩 현황'에서도 재사용 가능) */
    static FundingStatus buildFundingStatus(SqlSession session, int projectId) {
        ProjectSummary summary = session.getMapper(ProjectMapper.class).findSummaryById(projectId, AppConfig.today());
        List<RewardFundingStatus> rewards = session.getMapper(RewardMapper.class).findFundingStatusByProject(projectId);
        return new FundingStatus(summary, rewards);
    }

    private Project findOwnedProject(SqlSession session, LoginUser maker, int projectId) {
        Project project = session.getMapper(ProjectMapper.class).findById(projectId);
        require(project != null, projectId + "번 프로젝트가 존재하지 않습니다.");
        require(project.getMakerId() == maker.getUserId(), "본인의 프로젝트가 아닙니다.");
        return project;
    }

    /** 프로젝트 입력값 검사 후, 검사된 값만 담은 새 객체를 돌려준다 */
    private Project validateProjectInfo(Project input) {
        require(input != null, "프로젝트 정보를 입력하세요.");
        String name = Validator.requireText(input.getProjectName(), "프로젝트 이름", 1, 255);
        String description = Validator.optionalText(input.getProjectDescription(), "프로젝트 설명", 5000);
        require(input.getCategoryId() != null, "카테고리를 선택하세요.");
        require(input.getTargetAmount() > 0, "목표 금액은 1원 이상이어야 합니다.");
        require(input.getStartDate() != null && input.getEndDate() != null, "펀딩 시작일과 종료일을 입력하세요.");

        LocalDate today = AppConfig.today();
        require(!input.getStartDate().isBefore(today), "시작일은 오늘(" + today + ") 이후여야 합니다.");
        require(!input.getEndDate().isBefore(input.getStartDate()), "종료일은 시작일 이후여야 합니다.");

        return new Project(name, description, input.getCategoryId(), input.getTargetAmount(),
                input.getStartDate(), input.getEndDate());
    }

    private void validateImage(Reward input, Reward target) {
        byte[] image = input.getRewardImage();
        if (image == null || image.length == 0) {
            return;   // 이미지는 선택 사항
        }
        require(image.length <= AppConfig.MAX_IMAGE_BYTES, "이미지는 5MB 이하만 등록할 수 있습니다.");
        String type = Validator.requireText(input.getRewardImageType(), "이미지 형식", 1, 20);
        require(type.startsWith("image/"), "이미지 형식은 image/png, image/jpeg 처럼 입력하세요.");
        target.setRewardImage(image);
        target.setRewardImageType(type);
    }
}
