package crowdfunding.model.dto;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 서포터 개인정보 수정 요청 (MyPage)
 * newPassword 가 null(또는 빈 값)이면 비밀번호는 바꾸지 않는다.
 * 비밀번호를 바꿀 때는 본인 확인을 위해 currentPassword 가 필요하다.
 */
public class ProfileForm implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userName;
    private String phoneNumber;
    private String deliveryAddress;
    private LocalDate birthDate;
    private String currentPassword;
    private String newPassword;

    public ProfileForm() {
    }

    public ProfileForm(String userName, String phoneNumber, String deliveryAddress, LocalDate birthDate) {
        this.userName = userName;
        this.phoneNumber = phoneNumber;
        this.deliveryAddress = deliveryAddress;
        this.birthDate = birthDate;
    }

    /** 비밀번호도 함께 변경 */
    public ProfileForm changePassword(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        return this;
    }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
