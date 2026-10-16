package crowdfunding.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * [검사 5] MyPage 개인정보 수정 입력값.
 * null 로 둔 항목은 바꾸지 않고 기존 값을 유지한다.
 *
 *   userName, phoneNumber     → user 테이블
 *   currentPassword           → 비밀번호를 바꿀 때만 필요 (현재 비밀번호 확인)
 *   newPassword               → user 테이블 (null 이면 비밀번호 유지)
 *   deliveryAddress, birthDate → supporter 테이블
 *
 * [담당] D (검사 05)
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor   // new MyInfoForm(이름, 연락처, 현재비번, 새비번, 배송지, 생년월일) - 필드 순서대로
public class MyInfoForm {

    private String userName;
    private String phoneNumber;
    @ToString.Exclude   // 비밀번호는 출력하지 않는다
    private String currentPassword;
    @ToString.Exclude   // 비밀번호는 출력하지 않는다
    private String newPassword;
    private String deliveryAddress;
    private LocalDate birthDate;
}
