package crowdfunding.view;

import crowdfunding.model.domain.Category;
import crowdfunding.model.domain.Review;
import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.dto.ProfileForm;
import crowdfunding.model.dto.ProjectSearchCondition;
import crowdfunding.model.dto.SupporterProfile;
import crowdfunding.model.enums.FundingState;
import crowdfunding.model.enums.ProjectSort;
import crowdfunding.network.client.ServerConnection;
import crowdfunding.protocol.Command;

import java.time.LocalDate;
import java.util.List;

/** [2차] 서포터 메뉴 */
public class SupporterMenu extends BaseMenu {

    public SupporterMenu(ServerConnection conn, ConsoleInput in, LoginUser user) {
        super(conn, in, user);
    }

    @Override
    protected String title() {
        return "서포터 메뉴";
    }

    @Override
    protected void printMenu() {
        System.out.println("  1. 프로젝트 보기 (전체)");
        System.out.println("  2. 프로젝트 검색 / 필터링 / 정렬");
        System.out.println("  3. 프로젝트 상세 보기 및 리워드 신청");
        System.out.println("  4. 리뷰 및 별점 등록");
        System.out.println("  5. MyPage - 내 정보 보기");
        System.out.println("  6. MyPage - 개인정보 수정");
        // [2차 추가] 7. 후원 취소   8. 좋아요/좋아요 취소   9. MyPage - 후원 내역   (로그인 화면) 회원가입
    }

    @Override
    protected boolean handle(int choice) {
        switch (choice) {
            case 1: showProjects(null); showProjectDetail(null); return true;
            case 2: searchProjects(); return true;
            case 3: showProjectDetail(null); return true;
            case 4: registerReview(); return true;
            case 5: showProfile(); return true;
            case 6: updateProfile(); return true;
            default: return false;
        }
    }

    /** 검색어 + 카테고리 + 진행/마감 필터 + 정렬 → 서버의 MyBatis 동적 쿼리 */
    private void searchProjects() {
        List<Category> categories = loadCategories();
        if (categories == null) {
            return;
        }
        ResultView.title("프로젝트 검색");
        String keyword = in.optionalText("프로젝트명 검색어(엔터=전체): ");
        ResultView.printCategories(categories);
        Integer categoryId = in.optionalNumber("카테고리 번호(엔터=전체): ");
        int state = in.numberInRange("진행 상태 (0=전체, 1=펀딩중, 2=펀딩종료): ", 0, 2);
        int sort = in.numberInRange("정렬 (1=최신순, 2=모집금액순, 3=마감임박순, 4=좋아요순): ", 1, 4);

        ProjectSearchCondition condition = new ProjectSearchCondition(keyword, categoryId,
                FundingState.values()[state], ProjectSort.values()[sort - 1]);
        showProjects(condition);
        showProjectDetail(null);
    }

    private void registerReview() {
        ResultView.title("리뷰 및 별점 등록");
        System.out.println("  * 펀딩에 성공한 프로젝트 중 내가 후원한 프로젝트에만 작성할 수 있습니다.");
        int projectId = in.number("프로젝트 번호: ");
        int rating = in.numberInRange("별점(1~5): ", 1, 5);
        String content = in.optionalText("리뷰 내용: ");
        requestAndPrint(Command.SUPPORTER_REGISTER_REVIEW, new Review(projectId, rating, content));
    }

    private SupporterProfile showProfile() {
        SupporterProfile profile = request(Command.SUPPORTER_GET_PROFILE, null);
        if (profile != null) {
            ResultView.title("내 정보");
            ResultView.printProfile(profile);
        }
        return profile;
    }

    /** 각 항목은 엔터를 치면 기존 값 유지 */
    private void updateProfile() {
        SupporterProfile current = showProfile();
        if (current == null) {
            return;
        }
        System.out.println("  (엔터 = 기존 값 유지)");
        String name = orDefault(in.optionalText("이름: "), current.getUserName());
        String phone = orDefault(in.optionalText("연락처(010-0000-0000): "), current.getPhoneNumber());
        String address = orDefault(in.optionalText("배송지: "), current.getDeliveryAddress());
        LocalDate birth = in.optionalDate("생년월일(yyyy-MM-dd): ");
        ProfileForm form = new ProfileForm(name, phone, address, birth == null ? current.getBirthDate() : birth);

        String newPassword = in.optionalText("새 비밀번호(엔터=변경 안 함): ");
        if (newPassword != null) {
            form.changePassword(in.text("현재 비밀번호: "), newPassword);
        }
        requestAndPrint(Command.SUPPORTER_UPDATE_PROFILE, form);
    }

    private static String orDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }
}
