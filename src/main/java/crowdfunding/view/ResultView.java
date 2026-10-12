package crowdfunding.view;

import crowdfunding.config.AppConfig;
import crowdfunding.model.domain.Category;
import crowdfunding.model.dto.FundingStatus;
import crowdfunding.model.dto.FundingSupporter;
import crowdfunding.model.dto.ProjectDetail;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.model.dto.RewardFundingStatus;
import crowdfunding.model.dto.RewardView;
import crowdfunding.model.dto.ReviewView;
import crowdfunding.model.dto.SupporterProfile;
import crowdfunding.model.enums.ApprovalStatus;
import crowdfunding.protocol.Response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 결과 출력 전용 View (콘솔).
 * 1차 검사 코드와 2차 클라이언트 메뉴가 <b>같은 출력 메소드</b>를 사용한다.
 * 데이터가 컨트롤러에서 바로 왔든(1차) 소켓으로 왔든(2차) 같은 DTO 이므로 출력 코드는 바뀌지 않는다.
 */
public final class ResultView {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ResultView() {
    }

    // ------------------------------------------------------------ 공통

    public static void title(String title) {
        System.out.println();
        System.out.println("==================== " + title + " ====================");
    }

    public static void section(String text) {
        System.out.println();
        System.out.println("--- " + text + " ---");
    }

    public static void printResponse(Response<?> response) {
        System.out.println(response);
    }

    public static String won(long amount) {
        return String.format("%,d원", amount);
    }

    private static String percent(double rate) {
        return String.format("%.1f%%", rate);
    }

    private static String dateTime(LocalDateTime t) {
        return t == null ? "-" : t.format(DATE_TIME);
    }

    private static String quantity(RewardView r) {
        if (!r.isLimited()) {
            return "무제한 (신청 " + r.getAppliedCount() + "개)";
        }
        return "제공 " + r.getRewardAmount() + "개 중 " + r.getAppliedCount() + "개 신청 → 잔여 "
                + r.getRemaining() + "개" + (r.isSoldOut() ? " [매진]" : "");
    }

    private static String dDay(ProjectSummary p) {
        if (!p.isFundingOpen()) {
            return "";
        }
        long days = ChronoUnit.DAYS.between(AppConfig.today(), p.getEndDate());
        return days == 0 ? " (D-Day)" : " (D-" + days + ")";
    }

    public static void printCategories(List<Category> categories) {
        StringBuilder sb = new StringBuilder("  카테고리: ");
        for (Category c : categories) {
            sb.append(c.getCategoryId()).append(". ").append(c.getCategoryName()).append("   ");
        }
        System.out.println(sb.toString().trim());
    }

    // ------------------------------------------------------------ 프로젝트 목록

    /** 서포터/공통 프로젝트 목록 */
    public static void printProjectList(List<ProjectSummary> list) {
        if (list == null || list.isEmpty()) {
            System.out.println("  (조회된 프로젝트가 없습니다)");
            return;
        }
        for (ProjectSummary p : list) {
            System.out.printf("  [%d] %s | %s | 메이커 %s%n",
                    p.getProjectId(), p.getProjectName(), p.getCategoryName(), p.getMakerName());
            System.out.printf("       %s%s | %s ~ %s | 목표 %s | 모인 금액 %s (%s) | 후원자 %d명 | 좋아요 %d%n",
                    p.getStateLabel(), dDay(p), p.getStartDate().format(DATE), p.getEndDate().format(DATE),
                    won(p.getTargetAmount()), won(p.getTotalAmount()), percent(p.getAchievementRate()),
                    p.getSupporterCount(), p.getLikeCount());
        }
    }

    /** 메이커의 내 프로젝트 / 관리자의 승인 대기 목록 — 승인 상태와 거절 사유를 함께 보여준다 */
    public static void printManagedProjectList(List<ProjectSummary> list) {
        if (list == null || list.isEmpty()) {
            System.out.println("  (프로젝트가 없습니다)");
            return;
        }
        for (ProjectSummary p : list) {
            System.out.printf("  [%d] %s | %s | 메이커 %s | 등록 %s%n",
                    p.getProjectId(), p.getProjectName(), p.getCategoryName(), p.getMakerName(), dateTime(p.getCreatedAt()));
            System.out.printf("       승인 상태: %s | 진행 상태: %s | 리워드 %d개 | %s ~ %s | 목표 %s | 모인 금액 %s (%s)%n",
                    p.getApprovalStatus().getLabel(),
                    p.getApprovalStatus() == ApprovalStatus.APPROVED ? p.getStateLabel() + dDay(p) : "-",
                    p.getRewardCount(), p.getStartDate().format(DATE), p.getEndDate().format(DATE),
                    won(p.getTargetAmount()), won(p.getTotalAmount()), percent(p.getAchievementRate()));
            if (p.getApprovalStatus() == ApprovalStatus.REJECT && p.getRejectReason() != null) {
                System.out.println("       거절 사유: " + p.getRejectReason());
            }
        }
    }

    // ------------------------------------------------------------ 상세 / 현황

    public static void printProjectDetail(ProjectDetail d) {
        ProjectSummary p = d.getSummary();
        title("[" + p.getProjectId() + "] " + p.getProjectName());
        System.out.printf("  카테고리: %s | 메이커: %s | 상태: %s%s%n",
                p.getCategoryName(), p.getMakerName(), p.getStateLabel(), dDay(p));
        System.out.printf("  펀딩 기간: %s ~ %s%n", p.getStartDate().format(DATE), p.getEndDate().format(DATE));
        System.out.printf("  목표 금액: %s | 모인 금액: %s (달성률 %s) | 후원자 %d명 | 좋아요 %d%n",
                won(p.getTargetAmount()), won(p.getTotalAmount()), percent(p.getAchievementRate()),
                p.getSupporterCount(), p.getLikeCount());
        System.out.println("  설명: " + (d.getProjectDescription() == null ? "-" : d.getProjectDescription()));

        section("리워드");
        if (d.getRewards() == null || d.getRewards().isEmpty()) {
            System.out.println("  (등록된 리워드가 없습니다)");
        } else {
            for (RewardView r : d.getRewards()) {
                boolean mine = d.getMyRewardId() != null && d.getMyRewardId() == r.getRewardId();
                System.out.printf("  [%d] %s - %s%s%n", r.getRewardId(), r.getRewardName(), won(r.getRewardPrice()),
                        mine ? "   ← 내가 신청한 리워드" : "");
                System.out.println("       제공 내용: " + (r.getRewardDescription() == null ? "-" : r.getRewardDescription()));
                System.out.println("       제공 시기: " + (r.getRewardDeliveryDate() == null ? "-" : r.getRewardDeliveryDate().format(DATE))
                        + " | 수량: " + quantity(r)
                        + (r.getImageSize() == null ? "" : " | 이미지 " + r.getRewardImageType() + " " + (r.getImageSize() / 1024) + "KB"));
            }
        }
        if (d.getMyRewardId() != null || d.isLikedByMe()) {
            System.out.println("  * 나의 상태: " + (d.getMyRewardId() != null ? "후원함" : "후원 안 함")
                    + " / " + (d.isLikedByMe() ? "좋아요 누름" : "좋아요 안 누름"));
        }
    }

    /** 메이커의 프로젝트별 펀딩 현황 (총 펀딩 금액, 리워드별 신청/잔여 수량, 서포터 및 배송지) */
    public static void printFundingStatus(FundingStatus status) {
        ProjectSummary p = status.getProject();
        title("[" + p.getProjectId() + "] " + p.getProjectName() + " - 펀딩 현황");
        System.out.printf("  상태: %s%s | 기간: %s ~ %s%n", p.getStateLabel(), dDay(p),
                p.getStartDate().format(DATE), p.getEndDate().format(DATE));
        System.out.printf("  총 펀딩 금액: %s / 목표 %s (달성률 %s) | 후원자 %d명%n",
                won(p.getTotalAmount()), won(p.getTargetAmount()), percent(p.getAchievementRate()), p.getSupporterCount());

        for (RewardFundingStatus r : status.getRewards()) {
            System.out.printf("%n  리워드 [%d] %s (%s)%n", r.getRewardId(), r.getRewardName(), won(r.getRewardPrice()));
            System.out.println("     수량: " + quantity(r) + " | 리워드 합계 " + won((long) r.getRewardPrice() * r.getAppliedCount()));
            if (r.getSupporters().isEmpty()) {
                System.out.println("     (신청한 서포터 없음)");
            }
            for (FundingSupporter s : r.getSupporters()) {
                System.out.printf("     - %s | %s | 배송지: %s | 신청일 %s%n",
                        s.getUserName(), s.getPhoneNumber(), s.getDeliveryAddress(), dateTime(s.getFundedDate()));
            }
        }
    }

    // ------------------------------------------------------------ 리뷰 / 계정

    /** 리워드명 / 별점 / 리뷰 */
    public static void printReviews(List<ReviewView> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            System.out.println("  (작성된 리뷰가 없습니다)");
            return;
        }
        for (ReviewView r : reviews) {
            System.out.printf("  [%s] %s (%d점) | %s | %s%n", r.getRewardName(), stars(r.getRating()), r.getRating(),
                    r.getUserName(), dateTime(r.getReviewAt()));
            System.out.println("       " + (r.getReviewContent() == null ? "(내용 없음)" : r.getReviewContent()));
        }
    }

    private static String stars(int rating) {
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }

    public static void printProfile(SupporterProfile p) {
        System.out.println("  아이디  : " + p.getLoginId());
        System.out.println("  이름    : " + p.getUserName());
        System.out.println("  연락처  : " + p.getPhoneNumber());
        System.out.println("  배송지  : " + p.getDeliveryAddress());
        System.out.println("  생년월일: " + (p.getBirthDate() == null ? "-" : p.getBirthDate().format(DATE)));
    }
}
