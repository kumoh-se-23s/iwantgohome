package crowdfunding.model.service;

import crowdfunding.config.AppConfig;
import crowdfunding.config.TransactionManager;
import crowdfunding.model.dao.FundingMapper;
import crowdfunding.model.dao.ProjectMapper;
import crowdfunding.model.dao.ReviewMapper;
import crowdfunding.model.dao.RewardMapper;
import crowdfunding.model.dao.UserMapper;
import crowdfunding.model.domain.Funding;
import crowdfunding.model.domain.Project;
import crowdfunding.model.domain.Review;
import crowdfunding.model.domain.Reward;
import crowdfunding.model.domain.User;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProfileForm;
import crowdfunding.model.dto.SupporterProfile;
import crowdfunding.model.enums.ApprovalStatus;
import crowdfunding.model.enums.ProjectStatus;
import org.apache.ibatis.exceptions.PersistenceException;

import java.time.LocalDate;

import static crowdfunding.model.service.Validator.require;

/**
 * 서포터 기능
 * <pre>
 *  1차: 개인정보 수정, 리워드 신청(★동시성 제어), 리뷰·별점 등록
 *  2차: 후원 취소(cancelFunding), 좋아요 토글(toggleLike), MyPage 후원 내역(getMyFundings)
 * </pre>
 */
public class SupporterService {

    public SupporterProfile getProfile(LoginUser supporter) {
        return TransactionManager.execute(session -> {
            SupporterProfile profile = session.getMapper(UserMapper.class).findSupporterProfile(supporter.getUserId());
            require(profile != null, "서포터 정보가 없습니다.");
            return profile;
        });
    }

    /** MyPage 개인정보 수정 (이름, 연락처, 배송지, 생년월일 + 선택적으로 비밀번호) */
    public void updateProfile(LoginUser supporter, ProfileForm form) {
        require(form != null, "수정할 정보를 입력하세요.");
        String userName = Validator.requireText(form.getUserName(), "이름", 2, 10);
        String phone = Validator.requirePhone(form.getPhoneNumber());
        String address = Validator.requireText(form.getDeliveryAddress(), "배송지", 1, 255);
        LocalDate birthDate = form.getBirthDate();
        require(birthDate == null || !birthDate.isAfter(AppConfig.today()), "생년월일이 미래일 수 없습니다.");

        boolean changePassword = !Validator.isBlank(form.getNewPassword());
        String newPassword = changePassword ? Validator.requirePassword(form.getNewPassword()) : null;

        TransactionManager.run(session -> {
            UserMapper mapper = session.getMapper(UserMapper.class);
            if (changePassword) {
                User user = mapper.findById(supporter.getUserId());
                require(user != null && user.getPassword().equals(form.getCurrentPassword()),
                        "현재 비밀번호가 일치하지 않아 비밀번호를 변경할 수 없습니다.");
            }
            // user 와 supporter 두 테이블을 한 트랜잭션으로 수정
            mapper.updateUserInfo(supporter.getUserId(), userName, phone, newPassword);
            int changed = mapper.updateSupporterInfo(supporter.getUserId(), address, birthDate);
            require(changed == 1, "서포터 정보가 없습니다.");
        });
    }

    /**
     * ★ 리워드 신청 (후원) — 한정 수량 동시성 제어
     *
     * <pre>
     * 한 트랜잭션 안에서:
     *  1) 프로젝트가 펀딩 진행 중인지, 기간 안인지 확인
     *  2) 이미 이 프로젝트에 후원했는지 확인 (프로젝트당 1리워드)
     *  3) SELECT ... FOR UPDATE 로 리워드 행 잠금  ← 같은 리워드를 동시에 신청한 다른 요청은 여기서 대기
     *  4) 잔여 수량 확인 → 없으면 예외 → 롤백 (후원 기록 X, 재고 변경 X)
     *  5) applied_count + 1  (조건부 UPDATE: 수량이 남아 있을 때만)
     *  6) funding INSERT     (PK(user_id, project_id) 중복이면 실패 → 롤백)
     *  7) commit → 락 해제 → 대기하던 요청이 진행, 줄어든 수량을 보고 판단
     * </pre>
     * 최종 안전장치: DB CHECK(ck_reward_applied) 가 applied_count &lt;= reward_amount 를 보장.
     *
     * @return 신청 후의 리워드 정보 (잔여 수량 확인용)
     */
    public Reward applyReward(LoginUser supporter, int projectId, int rewardId) {
        return TransactionManager.execute(session -> {
            // 1) 프로젝트 상태
            Project project = session.getMapper(ProjectMapper.class).findById(projectId);
            require(project != null && project.getApprovalStatus() == ApprovalStatus.APPROVED,
                    projectId + "번 프로젝트는 공개된 프로젝트가 아닙니다.");
            require(project.getProjectStatus() == ProjectStatus.IN_PROGRESS, "펀딩이 종료된 프로젝트입니다.");
            LocalDate today = AppConfig.today();
            require(!today.isBefore(project.getStartDate()), "펀딩 시작일(" + project.getStartDate() + ") 이후에 후원할 수 있습니다.");
            require(!today.isAfter(project.getEndDate()), "펀딩 기간이 끝난 프로젝트입니다.");

            // 2) 프로젝트당 한 번만
            FundingMapper fundingMapper = session.getMapper(FundingMapper.class);
            require(fundingMapper.findByUserAndProject(supporter.getUserId(), projectId) == null,
                    "이미 후원한 프로젝트입니다. 한 프로젝트에는 하나의 리워드만 신청할 수 있습니다.");

            // 3) 리워드 행 잠금
            RewardMapper rewardMapper = session.getMapper(RewardMapper.class);
            Reward reward = rewardMapper.findByIdForUpdate(rewardId);
            require(reward != null && reward.getProjectId() == projectId,
                    rewardId + "번 리워드는 " + projectId + "번 프로젝트의 리워드가 아닙니다.");

            // 4) 잔여 수량
            require(!reward.isLimited() || reward.getRemaining() > 0,
                    "'" + reward.getRewardName() + "' 리워드의 잔여 수량이 없습니다. (매진)");

            // 5) 신청 수량 +1
            int changed = rewardMapper.increaseAppliedCount(rewardId);
            require(changed == 1, "'" + reward.getRewardName() + "' 리워드의 잔여 수량이 없습니다. (매진)");
            reward.setAppliedCount(reward.getAppliedCount() + 1);

            // 6) 후원 기록
            Funding funding = new Funding(projectId, rewardId);
            funding.setUserId(supporter.getUserId());
            try {
                fundingMapper.insertFunding(funding);
            } catch (PersistenceException e) {
                if (DbErrors.isDuplicateKey(e)) {   // 같은 서포터가 동시에 두 번 신청한 경우
                    throw new BusinessException("이미 후원한 프로젝트입니다. 한 프로젝트에는 하나의 리워드만 신청할 수 있습니다.", e);
                }
                throw e;
            }
            return reward;
        });
    }

    /** 리뷰·별점 등록 — 펀딩에 성공한(=배송 완료로 간주) 프로젝트 중 본인이 후원한 프로젝트에 1회 */
    public int registerReview(LoginUser supporter, Review input) {
        require(input != null && input.getProjectId() != null, "리뷰를 작성할 프로젝트를 선택하세요.");
        require(input.getRating() >= 1 && input.getRating() <= 5, "별점은 1~5점 중에서 선택하세요.");
        String content = Validator.optionalText(input.getReviewContent(), "리뷰 내용", 2000);
        int projectId = input.getProjectId();

        return TransactionManager.execute(session -> {
            Project project = session.getMapper(ProjectMapper.class).findById(projectId);
            require(project != null, projectId + "번 프로젝트가 존재하지 않습니다.");
            require(project.getProjectStatus() == ProjectStatus.SUCCESS,
                    "펀딩에 성공한 프로젝트에만 리뷰를 작성할 수 있습니다.");
            require(session.getMapper(FundingMapper.class).findByUserAndProject(supporter.getUserId(), projectId) != null,
                    "후원한 프로젝트에만 리뷰를 작성할 수 있습니다.");

            ReviewMapper reviewMapper = session.getMapper(ReviewMapper.class);
            require(!reviewMapper.existsByUserAndProject(supporter.getUserId(), projectId),
                    "이미 이 프로젝트에 리뷰를 작성했습니다.");

            Review review = new Review(projectId, input.getRating(), content);
            review.setUserId(supporter.getUserId());
            reviewMapper.insertReview(review);
            return review.getReviewId();
        });
    }

    // [2차] 후원 취소 — 펀딩 진행 중일 때만
    // public void cancelFunding(LoginUser supporter, int projectId) {
    //     funding 조회 → rewardMapper.findByIdForUpdate(rewardId) 로 잠금 → applied_count - 1 → funding DELETE
    // }

    // [2차] 좋아요 / 좋아요 취소 (한 버튼으로 토글)
    // public boolean toggleLike(LoginUser supporter, int projectId) {
    //     existsLike ? deleteLike : insertLike   (PK(project_id, user_id) 가 1인 1좋아요 보장)
    // }

    // [2차] MyPage 후원 내역: 프로젝트명, 신청한 리워드, 후원 금액, 신청일, 펀딩 상태, 리뷰 작성 여부
    // public List<MyFunding> getMyFundings(LoginUser supporter) { ... }
}
