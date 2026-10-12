package crowdfunding.model.domain;

import java.io.Serializable;
import java.time.LocalDate;

/** supporter 테이블 1행 (user 와 1:0..1 — 서포터만 배송지/생년월일을 가짐) */
public class Supporter implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer userId;           // user_id (PK 이자 user FK)
    private String deliveryAddress;   // delivery_address (배송지)
    private LocalDate birthDate;      // birth_date (NULL 허용)

    public Supporter() {
    }

    public Supporter(Integer userId, String deliveryAddress, LocalDate birthDate) {
        this.userId = userId;
        this.deliveryAddress = deliveryAddress;
        this.birthDate = birthDate;
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
}
