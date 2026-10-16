package crowdfunding.model.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * funding 테이블 한 행 (신청 현황).
 * PK = (user_id, project_id) → 한 서포터는 한 프로젝트에 한 번만 후원할 수 있다.
 * 후원 금액 컬럼은 없고, 신청한 리워드의 reward_price 가 곧 후원 금액이다.
 *
 * [담당] 공통 기반
 */
@Getter
@Setter
@ToString
public class Funding {

    private int userId;                // user_id     서포터
    private int projectId;             // project_id
    private int rewardId;              // reward_id   신청한 리워드
    private LocalDateTime fundedDate;  // funded_date 신청일
}
