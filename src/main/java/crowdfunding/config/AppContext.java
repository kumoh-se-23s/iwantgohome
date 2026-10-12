package crowdfunding.config;

import crowdfunding.controller.AdminController;
import crowdfunding.controller.AuthController;
import crowdfunding.controller.MakerController;
import crowdfunding.controller.ProjectController;
import crowdfunding.controller.RequestDispatcher;
import crowdfunding.controller.SupporterController;
import crowdfunding.model.service.AdminService;
import crowdfunding.model.service.AuthService;
import crowdfunding.model.service.FundingCloseService;
import crowdfunding.model.service.MakerService;
import crowdfunding.model.service.ProjectQueryService;
import crowdfunding.model.service.SupporterService;

/**
 * 서비스와 컨트롤러를 한 번만 만들어 연결해 두는 곳 (간단한 수동 DI).
 * 1차 Main(검사)과 2차 ServerMain 이 <b>같은 객체 구성</b>을 사용한다.
 * 서비스/컨트롤러는 상태(필드 값)를 바꾸지 않으므로 여러 스레드가 함께 써도 안전하다.
 */
public final class AppContext {

    private static final AppContext INSTANCE = new AppContext();

    private final FundingCloseService fundingCloseService = new FundingCloseService();

    private final AuthController authController;
    private final ProjectController projectController;
    private final MakerController makerController;
    private final AdminController adminController;
    private final SupporterController supporterController;
    private final RequestDispatcher dispatcher;

    private AppContext() {
        authController = new AuthController(new AuthService());
        projectController = new ProjectController(new ProjectQueryService());
        makerController = new MakerController(new MakerService());
        adminController = new AdminController(new AdminService());
        supporterController = new SupporterController(new SupporterService());
        dispatcher = new RequestDispatcher(authController, projectController, makerController,
                adminController, supporterController);
    }

    public static AppContext getInstance() {
        return INSTANCE;
    }

    public AuthController auth() { return authController; }
    public ProjectController project() { return projectController; }
    public MakerController maker() { return makerController; }
    public AdminController admin() { return adminController; }
    public SupporterController supporter() { return supporterController; }
    public RequestDispatcher dispatcher() { return dispatcher; }
    public FundingCloseService fundingCloseService() { return fundingCloseService; }
}
