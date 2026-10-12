package crowdfunding.model.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 리뷰 목록 한 줄 — 과제 조건 "리워드명 / 별점 / 리뷰" */
public class ReviewView implements Serializable {
    private static final long serialVersionUID = 1L;

    private int reviewId;
    private int projectId;
    private String userName;
    private String rewardName;   // 리뷰 작성자가 신청했던 리워드
    private int rating;
    private String reviewContent;
    private LocalDateTime reviewAt;

    public int getReviewId() { return reviewId; }
    public void setReviewId(int reviewId) { this.reviewId = reviewId; }
    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getRewardName() { return rewardName; }
    public void setRewardName(String rewardName) { this.rewardName = rewardName; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getReviewContent() { return reviewContent; }
    public void setReviewContent(String reviewContent) { this.reviewContent = reviewContent; }
    public LocalDateTime getReviewAt() { return reviewAt; }
    public void setReviewAt(LocalDateTime reviewAt) { this.reviewAt = reviewAt; }
}
