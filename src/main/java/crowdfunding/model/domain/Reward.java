package crowdfunding.model.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * reward 테이블 1행. 메이커의 리워드 등록 입력 데이터로도 사용.
 * <p>
 * 주의: DB 컬럼 reward_amount 는 "금액"이 아니라 <b>제공 가능 수량</b>이다. (NULL = 무제한)
 *       금액은 reward_price.
 */
public class Reward implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer rewardId;
    private Integer projectId;
    private String rewardName;               // 리워드 명칭
    private String rewardDescription;        // 제공 내용
    private int rewardPrice;                 // 후원에 필요한 금액
    private Integer rewardAmount;            // 제공 가능 수량 (NULL = 무제한)
    private int appliedCount;                // 지금까지 신청된 수량
    private LocalDateTime rewardDeliveryDate; // 제공 시기
    private byte[] rewardImage;              // 이미지 원본 (선택, 최대 5MB)
    private String rewardImageType;          // image/png, image/jpeg ...

    public Reward() {
    }

    /** 메이커가 리워드를 등록할 때 쓰는 생성자 (rewardAmount 가 null 이면 무제한) */
    public Reward(int projectId, String rewardName, String rewardDescription, int rewardPrice,
                  Integer rewardAmount, LocalDateTime rewardDeliveryDate) {
        this.projectId = projectId;
        this.rewardName = rewardName;
        this.rewardDescription = rewardDescription;
        this.rewardPrice = rewardPrice;
        this.rewardAmount = rewardAmount;
        this.rewardDeliveryDate = rewardDeliveryDate;
    }

    /** 수량 제한이 있는 리워드인가? */
    public boolean isLimited() {
        return rewardAmount != null;
    }

    /** 잔여 수량 (무제한이면 null) */
    public Integer getRemaining() {
        return rewardAmount == null ? null : rewardAmount - appliedCount;
    }

    public Integer getRewardId() { return rewardId; }
    public void setRewardId(Integer rewardId) { this.rewardId = rewardId; }
    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
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
    public byte[] getRewardImage() { return rewardImage; }
    public void setRewardImage(byte[] rewardImage) { this.rewardImage = rewardImage; }
    public String getRewardImageType() { return rewardImageType; }
    public void setRewardImageType(String rewardImageType) { this.rewardImageType = rewardImageType; }
}
