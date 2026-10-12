package crowdfunding.controller;

import crowdfunding.model.domain.Funding;
import crowdfunding.model.domain.Review;
import crowdfunding.model.domain.Reward;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProfileForm;
import crowdfunding.model.dto.SupporterProfile;
import crowdfunding.model.enums.Role;
import crowdfunding.model.service.SupporterService;
import crowdfunding.protocol.Response;

/** 서포터 요청 처리 */
public class SupporterController extends BaseController {

    private final SupporterService supporterService;

    public SupporterController(SupporterService supporterService) {
        this.supporterService = supporterService;
    }

    public Response<SupporterProfile> getProfile(LoginUser user) {
        return handle(() -> {
            requireRole(user, Role.SUPPORTER);
            return Response.ok(supporterService.getProfile(user));
        });
    }

    public Response<Void> updateProfile(LoginUser user, ProfileForm form) {
        return handle(() -> {
            requireRole(user, Role.SUPPORTER);
            supporterService.updateProfile(user, form);
            boolean pw = form.getNewPassword() != null && !form.getNewPassword().isBlank();
            return Response.ok("개인정보가 수정되었습니다." + (pw ? " (비밀번호 변경 포함)" : ""), null);
        });
    }

    /** 리워드 신청 — request 에는 projectId, rewardId 만 담겨 온다 (userId 는 로그인 정보 사용) */
    public Response<Reward> applyReward(LoginUser user, Funding request) {
        return handle(() -> {
            requireRole(user, Role.SUPPORTER);
            if (request == null || request.getProjectId() == null || request.getRewardId() == null) {
                return Response.fail("프로젝트 번호와 리워드 번호를 입력하세요.");
            }
            Reward reward = supporterService.applyReward(user, request.getProjectId(), request.getRewardId());
            String remain = reward.isLimited() ? reward.getRemaining() + "개" : "무제한";
            return Response.ok("'" + reward.getRewardName() + "' 리워드 신청 완료 (후원 금액 "
                    + String.format("%,d원", reward.getRewardPrice()) + ", 잔여 " + remain + ")", reward);
        });
    }

    public Response<Integer> registerReview(LoginUser user, Review review) {
        return handle(() -> {
            requireRole(user, Role.SUPPORTER);
            int reviewId = supporterService.registerReview(user, review);
            return Response.ok("리뷰가 등록되었습니다. (" + reviewId + "번)", reviewId);
        });
    }

    // [2차] cancelFunding(user, projectId), toggleLike(user, projectId), getMyFundings(user)
}
