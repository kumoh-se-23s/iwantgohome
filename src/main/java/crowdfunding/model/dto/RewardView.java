package crowdfunding.model.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 화면에 보여줄 리워드 정보 (이미지 원본 바이트는 빼고 크기만 담아 네트워크 전송량을 줄임).
 * 잔여 수량 = 제공 가능 수량 - 신청 수량 (무제한이면 null)
 */
public class RewardView implements Serializable {
    private static final long serialVersionUID = 1L;

    private int rewardId;
    private int projectId;
    private String rewardName;
    private String rewardDescription;
    private int rewardPrice;
    private Integer rewardAmount;            // 제공 가능 수량 (NULL = 무제한)
    private int appliedCount;                // 신청 수량
    private LocalDateTime rewardDeliveryDate;
    private String rewardImageType;
    private Integer imageSize;               // 이미지 바이트 수 (없으면 null)

    public boolean isLimited() {
        return rewardAmount != null;
    }

    /** 잔여 수량 (무제한이면 null) */
    public Integer getRemaining() {
        return rewardAmount == null ? null : rewardAmount - appliedCount;
    }

    public boolean isSoldOut() {
        return rewardAmount != null && appliedCount >= rewardAmount;
    }

    public int getRewardId() { return rewardId; }
    public void setRewardId(int rewardId) { this.rewardId = rewardId; }
    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    public String getRewardName() { return rewardName; }
    public void setRewardName(String rewardName) { this.rewardName = rewardName; }
    public String getRewardDescription() { return rewardDescription; }
    public void setRewardDescription(String rewardDescription) { this.rewardDescription = rewardDescription; }
    public int getRewardPrice() { return rewardPrice; }
    public void setRewardPrice(int rewardPrice) { this.rewardPrice = rewardPrice; }
    public Integer getRewardAmount() { return rewardAmount; }
    public void setRewardAmount(Integer rewardAmount) { this.rewardAmount = rewardAmount; }
    public int getAppliedCount() { return appliedCount; }
    public void setAppliedCount(int appliedCount) { this.appliedCount = appliedCount; }
    public LocalDateTime getRewardDeliveryDate() { return rewardDeliveryDate; }
    public void setRewardDeliveryDate(LocalDateTime rewardDeliveryDate) { this.rewardDeliveryDate = rewardDeliveryDate; }
    public String getRewardImageType() { return rewardImageType; }
    public void setRewardImageType(String rewardImageType) { this.rewardImageType = rewardImageType; }
    public Integer getImageSize() { return imageSize; }
    public void setImageSize(Integer imageSize) { this.imageSize = imageSize; }
}
