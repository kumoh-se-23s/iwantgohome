package crowdfunding.model.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

/** review 테이블 1행. 서포터의 리뷰 작성 요청 데이터로도 사용. */
public class Review implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer reviewId;
    private Integer userId;
    private Integer projectId;
    private int rating;               // 별점 1~5
    private String reviewContent;
    private LocalDateTime reviewAt;   // DB 기본값 CURRENT_TIMESTAMP

    public Review() {
    }

    /** 서포터의 리뷰 작성 요청 */
    public Review(int projectId, int rating, String reviewContent) {
        this.projectId = projectId;
        this.rating = rating;
        this.reviewContent = reviewContent;
    }

    public Integer getReviewId() { return reviewId; }
    public void setReviewId(Integer reviewId) { this.reviewId = reviewId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getReviewContent() { return reviewContent; }
    public void setReviewContent(String reviewContent) { this.reviewContent = reviewContent; }
    public LocalDateTime getReviewAt() { return reviewAt; }
    public void setReviewAt(LocalDateTime reviewAt) { this.reviewAt = reviewAt; }
}
