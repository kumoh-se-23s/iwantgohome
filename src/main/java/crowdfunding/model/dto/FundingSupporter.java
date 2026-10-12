package crowdfunding.model.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 메이커가 보는 "이 리워드를 신청한 서포터" 한 명 (배송지 포함) */
public class FundingSupporter implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String userName;
    private String phoneNumber;
    private String deliveryAddress;
    private LocalDateTime fundedDate;

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public LocalDateTime getFundedDate() { return fundedDate; }
    public void setFundedDate(LocalDateTime fundedDate) { this.fundedDate = fundedDate; }
}
