package crowdfunding.dbtest;

import crowdfunding.config.AppContext;
import crowdfunding.model.dto.LoginForm;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProjectDetail;
import crowdfunding.model.dto.RewardView;
import crowdfunding.protocol.Response;
import crowdfunding.view.ResultView;

/**
 * 1차 검사 클래스들의 공통 부분.
 * 검사 코드는 2차 서버와 <b>똑같은 컨트롤러</b>(AppContext)를 호출한다. 소켓만 거치지 않을 뿐이다.
 */
public abstract class DbTestSupport {

    protected final AppContext ctx = AppContext.getInstance();

    /** 이번 실행에서 수행된 검사 수 (Main 이 안내 문구를 띄울지 판단) */
    private static int executedCount = 0;

    public static int getExecutedCount() {
        return executedCount;
    }

    protected void header(String no, String role, String title) {
        executedCount++;
        ResultView.title("TEST " + no + " | " + role + " - " + title);
    }

    /** 실제 로그인 기능(AuthController)으로 로그인 */
    protected LoginUser login(String loginId, String password) {
        Response<LoginUser> res = ctx.auth().login(new LoginForm(loginId, password));
        System.out.println("로그인 " + loginId + " → " + res);
        if (!res.isSuccess()) {
            throw new IllegalStateException(loginId + " 로그인 실패 — initDB() 를 먼저 실행했는지 확인하세요.");
        }
        return res.getData();
    }

    /** MySQL Workbench 에서 테이블 변화를 확인할 SQL 안내 */
    protected void checkSql(String... sqls) {
        System.out.println();
        System.out.println(">> Workbench 확인 SQL");
        for (String sql : sqls) {
            System.out.println("   " + sql);
        }
    }

    /** 리워드 한 개의 현재 신청/잔여 수량 출력 */
    protected void printRewardQuantity(LoginUser viewer, int projectId, int rewardId, String when) {
        Response<ProjectDetail> res = ctx.project().getProjectDetail(viewer, projectId);
        if (!res.isSuccess()) {
            ResultView.printResponse(res);
            return;
        }
        for (RewardView r : res.getData().getRewards()) {
            if (r.getRewardId() == rewardId) {
                System.out.printf("  [%s] 리워드 %d '%s' : 제공 가능 %s / 신청 %d / 잔여 %s%n", when, r.getRewardId(),
                        r.getRewardName(), r.isLimited() ? r.getRewardAmount() + "개" : "무제한", r.getAppliedCount(),
                        r.isLimited() ? r.getRemaining() + "개" : "무제한");
            }
        }
    }
}
