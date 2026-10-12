package crowdfunding.model.dto;

import crowdfunding.model.enums.ApprovalStatus;
import crowdfunding.model.enums.ProjectStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 프로젝트 목록 한 줄에 필요한 정보 (project + category + user + 집계값).
 * 서포터 목록, 메이커의 내 프로젝트, 관리자의 승인 대기 목록이 모두 이 DTO 를 사용한다.
 */
public class ProjectSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    private int projectId;
    private String projectName;
    private int categoryId;
    private String categoryName;
    private int makerId;
    private String makerName;
    private ApprovalStatus approvalStatus;
    private ProjectStatus projectStatus;
    private String rejectReason;
    private LocalDate startDate;
    private LocalDate endDate;
    private int targetAmount;
    private LocalDateTime createdAt;

    // ---- 집계값 (SQL 에서 계산) ----
    private long totalAmount;      // 총 펀딩 금액 = 신청된 리워드 가격의 합
    private int supporterCount;    // 후원자 수 (= funding 행 수, 프로젝트당 1인 1후원)
    private int likeCount;         // 좋아요 수
    private int rewardCount;       // 등록된 리워드 수
    private boolean fundingOpen;   // 서포터 관점 '펀딩중' 여부 (진행 중 + 종료일 안 지남)

    /** 달성률(%) = 총 펀딩 금액 / 목표 금액 × 100 */
    public double getAchievementRate() {
        return targetAmount == 0 ? 0 : totalAmount * 100.0 / targetAmount;
    }

    /** 화면에 보여줄 상태 이름 */
    public String getStateLabel() {
        if (approvalStatus != ApprovalStatus.APPROVED) {
            return approvalStatus == null ? "-" : approvalStatus.getLabel();   // 작성 중 / 승인 대기 / 승인 거절
        }
        if (fundingOpen) return "펀딩중";
        if (projectStatus == ProjectStatus.SUCCESS) return "펀딩종료(성공)";
        if (projectStatus == ProjectStatus.FAIL) return "펀딩종료(실패)";
        return "펀딩종료(집계 전)";   // 종료일이 지났지만 아직 FundingCloseService 가 처리하기 전
    }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public int getMakerId() { return makerId; }
    public void setMakerId(int makerId) { this.makerId = makerId; }
    public String getMakerName() { return makerName; }
    public void setMakerName(String makerName) { this.makerName = makerName; }
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
    public ProjectStatus getProjectStatus() { return projectStatus; }
    public void setProjectStatus(ProjectStatus projectStatus) { this.projectStatus = projectStatus; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getTargetAmount() { return targetAmount; }
    public void setTargetAmount(int targetAmount) { this.targetAmount = targetAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(long totalAmount) { this.totalAmount = totalAmount; }
    public int getSupporterCount() { return supporterCount; }
    public void setSupporterCount(int supporterCount) { this.supporterCount = supporterCount; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public int getRewardCount() { return rewardCount; }
    public void setRewardCount(int rewardCount) { this.rewardCount = rewardCount; }
    public boolean isFundingOpen() { return fundingOpen; }
    public void setFundingOpen(boolean fundingOpen) { this.fundingOpen = fundingOpen; }
}
