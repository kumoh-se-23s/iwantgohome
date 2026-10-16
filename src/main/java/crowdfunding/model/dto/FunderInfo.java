package crowdfunding.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * [검사 1·8] 메이커가 보는 "나의 프로젝트에 펀딩한 서포터" 한 명.
 * 이름·연락처는 user 테이블, 배송지는 supporter 테이블에서 가져온다.
 *
 * [담당] D (검사 01)
 */
@Getter
@Setter
@ToString
public class FunderInfo {

    private int userId;
    private String userName;
    private String phoneNumber;
    private String deliveryAddress;
    private LocalDateTime fundedDate;
}
