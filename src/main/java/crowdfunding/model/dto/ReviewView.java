package crowdfunding.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * [검사 9] 리뷰 출력용: 리워드명 / 별점 / 리뷰
 * 리워드명은 review → funding(같은 서포터·프로젝트) → reward 로 찾아온다.
 *
 * [담당] C (검사 09)
 */
@Getter
@Setter
@ToString
public class ReviewView {

    private int reviewId;
    private String projectName;
    private String rewardName;
    private String userName;
    private int rating;
    private String reviewContent;
    private LocalDateTime reviewAt;

    /** 별점을 ★ 로 표시. 예) 4점 → ★★★★☆ */
    public String getStars() {
        int r = Math.max(0, Math.min(5, rating));
        return "★".repeat(r) + "☆".repeat(5 - r);
    }
}
