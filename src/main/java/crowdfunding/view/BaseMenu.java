package crowdfunding.view;

import crowdfunding.model.domain.Category;
import crowdfunding.model.domain.Funding;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProjectDetail;
import crowdfunding.model.dto.ProjectSearchCondition;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.model.dto.ReviewView;
import crowdfunding.model.enums.Role;
import crowdfunding.network.client.ServerConnection;
import crowdfunding.protocol.Command;
import crowdfunding.protocol.Response;

import java.io.Serializable;
import java.util.List;

/**
 * [2차] 역할별 메뉴의 공통 부분.
 * 메뉴를 고르면 기능을 실행하고 다시 같은 메뉴로 돌아온다 (프로그램 재시작 없이 계속 시연 가능).
 * 0 을 고르면 로그아웃 → 로그인 화면으로 돌아간다.
 */
public abstract class BaseMenu {

    protected final ServerConnection conn;
    protected final ConsoleInput in;
    protected final LoginUser user;

    protected BaseMenu(ServerConnection conn, ConsoleInput in, LoginUser user) {
        this.conn = conn;
        this.in = in;
        this.user = user;
    }

    public final void run() {
        while (conn.isConnected()) {
            System.out.println();
            System.out.println("========== " + title() + " | " + user + " ==========");
            printMenu();
            System.out.println("  0. 로그아웃");
            int choice = in.number("선택> ");
            if (choice == 0) {
                conn.send(Command.LOGOUT);
                System.out.println("로그아웃했습니다.");
                return;
            }
            if (!handle(choice)) {
                System.out.println("  메뉴 번호를 다시 선택하세요.");
            }
        }
    }

    protected abstract String title();

    protected abstract void printMenu();

    /** 메뉴 번호 처리. 없는 번호면 false */
    protected abstract boolean handle(int choice);

    /** 요청을 보내고, 실패면 메시지를 출력하고 null 을 돌려준다 */
    protected <T> T request(Command command, Serializable payload) {
        Response<T> response = conn.send(command, payload);
        if (!response.isSuccess()) {
            ResultView.printResponse(response);
            return null;
        }
        return response.getData();
    }

    /** 요청을 보내고 결과 메시지를 출력한다 (등록/수정/신청처럼 결과 메시지가 중요한 요청) */
    protected <T> Response<T> requestAndPrint(Command command, Serializable payload) {
        Response<T> response = conn.send(command, payload);
        ResultView.printResponse(response);
        return response;
    }

    // ------------------------------------------------------------ 공통 기능

    protected List<Category> loadCategories() {
        return request(Command.GET_CATEGORIES, null);
    }

    /** 프로젝트 보기 (기본: 전체, 최신순) */
    protected void showProjects(ProjectSearchCondition condition) {
        List<ProjectSummary> list = request(Command.SEARCH_PROJECTS, condition);
        if (list != null) {
            ResultView.title("프로젝트 목록 (" + (condition == null ? "전체" : condition.toString()) + ")");
            ResultView.printProjectList(list);
        }
    }

    /** 프로젝트 상세 보기 — 서포터라면 이어서 리워드 신청까지 */
    protected void showProjectDetail(Integer projectId) {
        if (projectId == null) {
            projectId = in.optionalNumber("상세 보기할 프로젝트 번호(엔터=메뉴로): ");
            if (projectId == null) {
                return;
            }
        }
        ProjectDetail detail = request(Command.GET_PROJECT_DETAIL, projectId);
        if (detail == null) {
            return;
        }
        ResultView.printProjectDetail(detail);

        List<ReviewView> reviews = request(Command.GET_PROJECT_REVIEWS, projectId);
        if (reviews != null && !reviews.isEmpty()) {
            ResultView.section("리뷰 (최신순)");
            ResultView.printReviews(reviews);
        }

        if (user.getRole() == Role.SUPPORTER && detail.getSummary().isFundingOpen() && detail.getMyRewardId() == null) {
            Integer rewardId = in.optionalNumber("신청할 리워드 번호(엔터=신청 안 함): ");
            if (rewardId != null) {
                requestAndPrint(Command.SUPPORTER_APPLY_REWARD, new Funding(projectId, rewardId));
            }
        }
    }
}
