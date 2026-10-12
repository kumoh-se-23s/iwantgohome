package crowdfunding.controller;

import crowdfunding.model.domain.Funding;
import crowdfunding.model.domain.Project;
import crowdfunding.model.domain.Review;
import crowdfunding.model.domain.Reward;
import crowdfunding.model.dto.LoginForm;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProfileForm;
import crowdfunding.model.dto.ProjectSearchCondition;
import crowdfunding.network.server.ClientSession;
import crowdfunding.protocol.Request;
import crowdfunding.protocol.Response;

/**
 * [2차] 네트워크로 받은 Request(Command + payload)를 알맞은 컨트롤러 메소드에 연결한다.
 * <p>
 * 1차 테스트 코드는 컨트롤러 메소드를 직접 호출하고,
 * 2차 서버는 이 디스패처를 거쳐 <b>똑같은 컨트롤러 메소드</b>를 호출한다.
 * → 2차에서 새 기능을 추가할 때: Command 추가 → 여기 case 한 줄 → 컨트롤러/서비스/매퍼 메소드 추가
 */
public class RequestDispatcher {

    private final AuthController auth;
    private final ProjectController project;
    private final MakerController maker;
    private final AdminController admin;
    private final SupporterController supporter;

    public RequestDispatcher(AuthController auth, ProjectController project, MakerController maker,
                             AdminController admin, SupporterController supporter) {
        this.auth = auth;
        this.project = project;
        this.maker = maker;
        this.admin = admin;
        this.supporter = supporter;
    }

    public Response<?> dispatch(Request request, ClientSession session) {
        if (request == null || request.getCommand() == null) {
            return Response.fail("잘못된 요청입니다.");
        }
        LoginUser user = session.getLoginUser();   // 서버가 보관한 로그인 정보 (클라이언트가 위조 불가)
        try {
            switch (request.getCommand()) {
                // ---------- 공통 ----------
                case LOGIN: {
                    Response<LoginUser> res = auth.login(payload(request, LoginForm.class));
                    if (res.isSuccess()) {
                        session.login(res.getData());
                    }
                    return res;
                }
                case LOGOUT:
                    session.logout();
                    return Response.ok("로그아웃되었습니다.", null);
                case GET_CATEGORIES:
                    return project.getCategories(user);
                case SEARCH_PROJECTS:
                    return project.searchProjects(user, payload(request, ProjectSearchCondition.class));
                case GET_PROJECT_DETAIL:
                    return project.getProjectDetail(user, intPayload(request));
                case GET_PROJECT_REVIEWS:
                    return project.getProjectReviews(user, intPayload(request));

                // ---------- 메이커 ----------
                case MAKER_REGISTER_PROJECT:
                    return maker.registerProject(user, payload(request, Project.class));
                case MAKER_ADD_REWARD:
                    return maker.addReward(user, payload(request, Reward.class));
                case MAKER_SUBMIT_PROJECT:
                    return maker.submitProject(user, intPayload(request));
                case MAKER_MY_PROJECTS:
                    return maker.getMyProjects(user);
                case MAKER_FUNDING_STATUS:
                    return maker.getFundingStatus(user, intPayload(request));

                // ---------- 관리자 ----------
                case ADMIN_WAITING_PROJECTS:
                    return admin.getWaitingProjects(user);
                case ADMIN_APPROVE_PROJECT:
                    return admin.approveProject(user, intPayload(request));

                // ---------- 서포터 ----------
                case SUPPORTER_GET_PROFILE:
                    return supporter.getProfile(user);
                case SUPPORTER_UPDATE_PROFILE:
                    return supporter.updateProfile(user, payload(request, ProfileForm.class));
                case SUPPORTER_APPLY_REWARD:
                    return supporter.applyReward(user, payload(request, Funding.class));
                case SUPPORTER_REGISTER_REVIEW:
                    return supporter.registerReview(user, payload(request, Review.class));

                // [2차] case SUPPORTER_TOGGLE_LIKE: return supporter.toggleLike(user, intPayload(request));
                default:
                    return Response.fail("아직 지원하지 않는 요청입니다: " + request.getCommand());
            }
        } catch (IllegalArgumentException e) {
            return Response.fail(e.getMessage());
        }
    }

    /** payload 를 기대한 타입으로 꺼낸다. 타입이 다르면 잘못된 요청 */
    private static <T> T payload(Request request, Class<T> type) {
        Object value = request.getPayload();
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("잘못된 요청 데이터입니다. (" + type.getSimpleName() + " 필요)");
        }
        return type.cast(value);
    }

    private static int intPayload(Request request) {
        Integer value = payload(request, Integer.class);
        if (value == null) {
            throw new IllegalArgumentException("번호를 입력하세요.");
        }
        return value;
    }
}
