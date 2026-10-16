package crowdfunding.model.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * reward 테이블 한 행.
 *
 * rewardAmount 는 "제공 가능 수량"이고 NULL 이면 수량 무제한이다.
 * NULL 을 담아야 해서 int 가 아니라 Integer 를 쓴다.
 *
 * [2차] 이미지(reward_image, reward_image_type)는 12월 데모 때 byte[] / String 필드로 추가한다.
 *
 * [담당] 공통 기반
 */
@Getter
@Setter
@ToString
public class Reward {

    private int rewardId;                     // reward_id            PK
    private String rewardName;                // reward_name          리워드 이름
    private Integer rewardAmount;             // reward_amount        제공 가능 수량 (NULL = 무제한)
    private int rewardPrice;                  // reward_price         후원 금액
    private int appliedCount;                 // applied_count        지금까지 신청된 수량
    private LocalDateTime rewardDeliveryDate; // reward_delivery_date 제공 시기
    private String rewardDescription;         // reward_description   제공 내용
    private int projectId;                    // project_id           FK → project

    /** 테스트·화면에서 리워드를 간단히 만들기 위한 생성 메소드 */
    public static Reward of(String name, String description, int price, Integer amount, LocalDateTime deliveryDate) {
        Reward r = new Reward();
        r.setRewardName(name);
        r.setRewardDescription(description);
        r.setRewardPrice(price);
        r.setRewardAmount(amount);
        r.setRewardDeliveryDate(deliveryDate);
        return r;
    }

    /** 수량이 제한된 리워드인지 */
    public boolean isLimited() {
        return rewardAmount != null;
    }

    /** 남은 수량 (무제한이면 null) */
    public Integer getRemaining() {
        return isLimited() ? rewardAmount - appliedCount : null;
    }

    /** 지금 한 개 더 신청할 수 있는지 */
    public boolean hasStock() {
        return !isLimited() || appliedCount < rewardAmount;
    }
}
