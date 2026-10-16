package crowdfunding.model.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * supporter 테이블 한 행 (user 와 1:1, PK = user_id).
 * 이름·연락처는 user 테이블에 있고, 여기에는 배송지와 생년월일만 있다.
 *
 * [담당] 공통 기반
 */
@Getter
@Setter
@ToString
public class Supporter {

    private int userId;              // user_id          PK, FK → user
    private String deliveryAddress;  // delivery_address 배송지
    private LocalDate birthDate;     // birth_date       생년월일 (NULL 가능)
}
