package crowdfunding.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * [검사 6·7] 서포터가 보는 프로젝트 목록의 한 줄.
 *
 * [담당] B (검사 06·07)
 */
@Getter
@Setter
@ToString
public class ProjectSummary {

    private int projectId;
    private String projectName;
    private String categoryName;
    private String makerName;
    private String projectStatus;     // DB 값 in_progress / success / fail
    private String supporterState;    // 서포터 관점 상태: 펀딩중 / 펀딩종료 (SQL 에서 계산)
    private LocalDate startDate;
    private LocalDate endDate;
    private int targetAmount;
    private long totalAmount;         // 모인 금액
    private int supporterCount;       // 후원자 수
    private int likeCount;            // 좋아요 수

    /** 달성률 (%) */
    public double getAchieveRate() {
        return targetAmount == 0 ? 0 : totalAmount * 100.0 / targetAmount;
    }
}
