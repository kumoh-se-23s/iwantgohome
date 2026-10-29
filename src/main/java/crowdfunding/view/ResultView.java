package crowdfunding.view;

import crowdfunding.model.domain.Funding;
import crowdfunding.model.domain.Project;
import crowdfunding.model.domain.Reward;
import crowdfunding.model.domain.Supporter;
import crowdfunding.model.domain.User;
import crowdfunding.model.dto.FunderInfo;
import crowdfunding.model.dto.FundingStatus;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.model.dto.RewardStatus;
import crowdfunding.model.dto.ReviewView;
import crowdfunding.model.enums.ApprovalStatus;
import crowdfunding.model.enums.ProjectStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 콘솔 출력 담당 (MVC 의 View).
 * 데이터를 받아서 보여 주기만 하고, 계산이나 판단은 하지 않는다.
 * 1차 검사와 12월 클라이언트 화면에서 같이 쓸 수 있다.
 *
 * [담당] D (공통 출력)
 */
public final class ResultView {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String LINE = "=".repeat(78);

    private ResultView() {
    }

    // ------------------------------------------------------------------ 공통

    public static void title(String text) {
        System.out.println();
        System.out.println(LINE);
        System.out.println(" " + text);
        System.out.println(LINE);
    }

    public static void section(String text) {
        System.out.println();
        System.out.println("▶ " + text);
    }

    public static void info(String text) {
        System.out.println("   " + text);
    }

    public static void success(String text) {
        System.out.println("   [성공] " + text);
    }

    public static void fail(String text) {
        System.out.println("   [실패] " + text);
    }

    /** Workbench 에서 그대로 붙여 넣어 확인할 SQL */
    public static void checkSql(String... sqls) {
        System.out.println();
        System.out.println("   ※ Workbench 확인 SQL");
        for (String sql : sqls) {
            System.out.println("     " + sql);
        }
    }

    public static String won(long amount) {
        return String.format("%,d원", amount);
    }

    private static String dateTime(LocalDateTime t) {
        return t == null ? "-" : t.format(DATE_TIME);
    }

    // ------------------------------------------------------------------ 검사 1 · 8: 펀딩 현황

    public static void fundingStatus(List<FundingStatus> list) {
        if (list.isEmpty()) {
            info("승인된 프로젝트가 없습니다.");
            return;
        }
        for (FundingStatus fs : list) {
            fundingStatus(fs);
        }
    }

    public static void fundingStatus(FundingStatus fs) {
        System.out.println();
        System.out.printf("   [프로젝트 %d] %s  | %s | %s ~ %s%n", fs.getProjectId(), fs.getProjectName(),
                ProjectStatus.labelOf(fs.getProjectStatus()), fs.getStartDate(), fs.getEndDate());
        System.out.printf("     총 펀딩 금액 %s / 목표 %s (달성률 %.1f%%)  |  후원 서포터 %d명%n",
                won(fs.getTotalAmount()), won(fs.getTargetAmount()), fs.getAchieveRate(), fs.getSupporterCount());

        for (RewardStatus r : fs.getRewards()) {
            String remain = r.getRewardAmount() == null
                    ? "잔여 무제한"
                    : "잔여 " + r.getRemaining() + "개 (총 " + r.getRewardAmount() + "개)";
            System.out.printf("     - 리워드 %d [%s] %s  |  신청 %d개 / %s%n",
                    r.getRewardId(), r.getRewardName(), won(r.getRewardPrice()), r.getAppliedCount(), remain);
            if (r.getFunders().isEmpty()) {
                System.out.println("         (아직 신청한 서포터 없음)");
            }
            for (FunderInfo f : r.getFunders()) {
                System.out.printf("         · %s (user %d)  %s  배송지: %s  [신청 %s]%n",
                        f.getUserName(), f.getUserId(), f.getPhoneNumber(), f.getDeliveryAddress(),
                        dateTime(f.getFundedDate()));
            }
        }
    }

    // ------------------------------------------------------------------ 검사 6 · 7: 프로젝트 목록

    public static void projects(List<ProjectSummary> list) {
        if (list.isEmpty()) {
            info("조건에 맞는 프로젝트가 없습니다.");
            return;
        }
        System.out.println("   " + pad("번호", 4) + pad("상태", 10) + pad("카테고리", 11) + pad("프로젝트명", 26)
                + pad("모인 금액 / 목표 (달성률)", 34) + pad("마감일", 12) + pad("후원", 6) + "좋아요");
        System.out.println("   " + "-".repeat(110));
        for (ProjectSummary p : list) {
            String money = won(p.getTotalAmount()) + " / " + won(p.getTargetAmount())
                    + String.format(" (%.1f%%)", p.getAchieveRate());
            System.out.println("   " + pad(String.valueOf(p.getProjectId()), 4) + pad(p.getSupporterState(), 10)
                    + pad(p.getCategoryName(), 11) + pad(p.getProjectName(), 26) + pad(money, 34)
                    + pad(String.valueOf(p.getEndDate()), 12) + pad(p.getSupporterCount() + "명", 6)
                    + p.getLikeCount());
        }
        info("총 " + list.size() + "개");
    }

    // ------------------------------------------------------------------ 단건 출력

    public static void project(String label, Project p) {
        if (p == null) {
            info(label + ": (없음)");
            return;
        }
        info(String.format("%s: 프로젝트 %d [%s]  승인 상태=%s(%s)  진행 상태=%s  기간 %s ~ %s  목표 %s",
                label, p.getProjectId(), p.getProjectName(),
                ApprovalStatus.labelOf(p.getApprovalStatus()), p.getApprovalStatus(),
                p.getProjectStatus() == null ? "NULL" : ProjectStatus.labelOf(p.getProjectStatus()),
                p.getStartDate(), p.getEndDate(), won(p.getTargetAmount())));
    }


    public static void reward(String label, Reward r) {
        if (r == null) {
            info(label + ": (없음)");
            return;
        }
        String amount = r.isLimited()
                ? "신청 " + r.getAppliedCount() + "개 / 수량 " + r.getRewardAmount() + "개 → 잔여 " + r.getRemaining() + "개"
                : "신청 " + r.getAppliedCount() + "개 / 수량 무제한";
        info(String.format("%s: 리워드 %d [%s] %s  |  %s", label, r.getRewardId(), r.getRewardName(),
                won(r.getRewardPrice()), amount));
    }

    public static void funding(String label, Funding f) {
        if (f == null) {
            info(label + ": 후원 기록 없음");
            return;
        }
        info(String.format("%s: 서포터 %d → 프로젝트 %d, 리워드 %d, 신청일 %s",
                label, f.getUserId(), f.getProjectId(), f.getRewardId(), dateTime(f.getFundedDate())));
    }

    public static void profile(String label, User u, Supporter s) {
        info(String.format("%s: [user %d] 이름=%s, 연락처=%s, 비밀번호=%s, 배송지=%s, 생년월일=%s",
                label, u.getUserId(), u.getUserName(), u.getPhoneNumber(), u.getPw(),
                s == null ? "-" : s.getDeliveryAddress(),
                s == null || s.getBirthDate() == null ? "-" : s.getBirthDate()));
    }

    public static void review(ReviewView v) {
        if (v == null) {
            info("리뷰 없음");
            return;
        }
        info("프로젝트 : " + v.getProjectName());
        info("작성자   : " + v.getUserName() + "  (" + dateTime(v.getReviewAt()) + ")");
        info("리워드명 : " + v.getRewardName());
        info("별점     : " + v.getStars() + " (" + v.getRating() + "점)");
        info("리뷰     : " + (v.getReviewContent() == null ? "(내용 없음)" : v.getReviewContent()));
    }

    // ------------------------------------------------------------------ 한글 정렬

    /**
     * 콘솔에서 한글은 영문보다 두 칸을 차지해서 String.format("%-10s") 로는 줄이 안 맞는다.
     * 화면에 보이는 폭을 직접 세서 공백을 채운다.
     */
    private static String pad(String s, int width) {
        String text = s == null ? "-" : s;
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            w += (c >= 0x1100 && c <= 0x11FF) || (c >= 0x3130 && c <= 0x318F)
                    || (c >= 0xAC00 && c <= 0xD7A3) || (c >= 0xFF00 && c <= 0xFFEF) ? 2 : 1;
        }
        return w >= width ? text + " " : text + " ".repeat(width - w);
    }
}
