package crowdfunding.model.dto;

import java.io.Serializable;
import java.time.LocalDate;

/** 서포터 MyPage 계정 정보 (user + supporter 조인 결과, 비밀번호 제외) */
public class SupporterProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String loginId;
    private String userName;
    private String phoneNumber;
    private String deliveryAddress;
    private LocalDate birthDate;

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
}
