package crowdfunding.view;

import crowdfunding.config.AppConfig;
import crowdfunding.model.domain.Category;
import crowdfunding.model.domain.Project;
import crowdfunding.model.domain.Reward;
import crowdfunding.model.dto.FundingStatus;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.model.enums.ApprovalStatus;
import crowdfunding.network.client.ServerConnection;
import crowdfunding.protocol.Command;
import crowdfunding.protocol.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/** [2차] 메이커 메뉴 */
public class MakerMenu extends BaseMenu {

    public MakerMenu(ServerConnection conn, ConsoleInput in, LoginUser user) {
        super(conn, in, user);
    }

    @Override
    protected String title() {
        return "메이커 메뉴";
    }

    @Override
    protected void printMenu() {
        System.out.println("  1. 프로젝트 등록 (기본 정보 → 리워드 → 승인 신청)");
        System.out.println("  2. 리워드 추가 등록");
        System.out.println("  3. 프로젝트 승인 신청 / 재신청");
        System.out.println("  4. 내 프로젝트 및 승인 상태 조회");
        System.out.println("  5. 프로젝트별 펀딩 현황");
        System.out.println("  6. 전체 프로젝트 보기");
        System.out.println("  7. 프로젝트 상세 보기");
        // [2차 추가] 8. 거절된 프로젝트 수정   9. 프로젝트 리뷰 조회
    }

    @Override
    protected boolean handle(int choice) {
        switch (choice) {
            case 1: registerProject(); return true;
            case 2: addRewardToProject(); return true;
            case 3: submitProject(); return true;
            case 4: showMyProjects(); return true;
            case 5: showFundingStatus(); return true;
            case 6: showProjects(null); return true;
            case 7: showProjectDetail(null); return true;
            default: return false;
        }
    }

    private void registerProject() {
        List<Category> categories = loadCategories();
        if (categories == null) {
            return;
        }
        ResultView.title("프로젝트 기본 정보 등록");
        ResultView.printCategories(categories);
        String name = in.text("프로젝트 이름: ");
        String description = in.optionalText("프로젝트 설명(엔터=생략): ");
        int categoryId = in.number("카테고리 번호: ");
        int target = in.number("목표 금액(원): ");
        LocalDate start = in.date("시작일(yyyy-MM-dd, 엔터=오늘): ", AppConfig.today());
        LocalDate end = in.date("종료일(yyyy-MM-dd): ", null);

        Response<Integer> res = requestAndPrint(Command.MAKER_REGISTER_PROJECT,
                new Project(name, description, categoryId, target, start, end));
        if (!res.isSuccess()) {
            return;
        }
        int projectId = res.getData();
        do {
            addReward(projectId);
        } while (in.confirm("리워드를 더 등록할까요?"));

        if (in.confirm("지금 관리자에게 승인 신청할까요?")) {
            requestAndPrint(Command.MAKER_SUBMIT_PROJECT, projectId);
        }
    }

    private void addRewardToProject() {
        Integer projectId = chooseEditableProject("리워드를 등록할 프로젝트 번호: ");
        if (projectId != null) {
            addReward(projectId);
        }
    }

    private void addReward(int projectId) {
        ResultView.section(projectId + "번 프로젝트 리워드 등록");
        String name = in.text("리워드 이름: ");
        String description = in.optionalText("제공 내용(엔터=생략): ");
        int price = in.number("후원 금액(원): ");
        Integer amount = in.optionalNumber("제공 가능 수량(엔터=무제한): ");
        LocalDate delivery = in.optionalDate("제공 시기(yyyy-MM-dd, 엔터=미정): ");
        Reward reward = new Reward(projectId, name, description, price, amount,
                delivery == null ? null : delivery.atStartOfDay());
        attachImage(reward, in.optionalText("이미지 파일 경로(엔터=없음): "));
        requestAndPrint(Command.MAKER_ADD_REWARD, reward);
    }

    /** 이미지 파일을 읽어 리워드에 담는다 (클라이언트 PC 의 파일 → 바이트로 서버 전송) */
    private void attachImage(Reward reward, String path) {
        if (path == null) {
            return;
        }
        try {
            Path file = Path.of(path);
            String type = Files.probeContentType(file);
            if (type == null) {
                String lower = path.toLowerCase();
                type = lower.endsWith(".png") ? "image/png"
                        : (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) ? "image/jpeg"
                        : lower.endsWith(".gif") ? "image/gif" : null;
            }
            if (type == null || !type.startsWith("image/")) {
                System.out.println("  이미지 파일이 아니어서 이미지 없이 등록합니다.");
                return;
            }
            reward.setRewardImage(Files.readAllBytes(file));
            reward.setRewardImageType(type);
        } catch (IOException | RuntimeException e) {
            System.out.println("  이미지 파일을 읽지 못해 이미지 없이 등록합니다. (" + e.getMessage() + ")");
        }
    }

    private void submitProject() {
        Integer projectId = chooseEditableProject("승인 신청할 프로젝트 번호: ");
        if (projectId != null) {
            requestAndPrint(Command.MAKER_SUBMIT_PROJECT, projectId);
        }
    }

    private void showMyProjects() {
        List<ProjectSummary> list = request(Command.MAKER_MY_PROJECTS, null);
        if (list != null) {
            ResultView.title("내 프로젝트");
            ResultView.printManagedProjectList(list);
        }
    }

    private void showFundingStatus() {
        showMyProjects();
        Integer projectId = in.optionalNumber("펀딩 현황을 볼 프로젝트 번호(엔터=메뉴로): ");
        if (projectId == null) {
            return;
        }
        FundingStatus status = request(Command.MAKER_FUNDING_STATUS, projectId);
        if (status != null) {
            ResultView.printFundingStatus(status);
        }
    }

    /** 작성 중 / 거절된 내 프로젝트를 보여주고 번호를 고르게 한다 */
    private Integer chooseEditableProject(String prompt) {
        List<ProjectSummary> list = request(Command.MAKER_MY_PROJECTS, null);
        if (list == null) {
            return null;
        }
        List<ProjectSummary> editable = list.stream()
                .filter(p -> p.getApprovalStatus() == ApprovalStatus.DRAFT || p.getApprovalStatus() == ApprovalStatus.REJECT)
                .collect(Collectors.toList());
        ResultView.title("작성 중 / 거절된 프로젝트");
        ResultView.printManagedProjectList(editable);
        if (editable.isEmpty()) {
            return null;
        }
        return in.optionalNumber(prompt.replace(": ", "(엔터=취소): "));
    }
}
