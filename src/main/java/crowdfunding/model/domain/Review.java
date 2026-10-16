package crowdfunding.model.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * review 테이블 한 행.
 * (user_id, project_id) 가 funding 을 참조하므로 후원 기록이 없으면 DB 가 INSERT 를 거절한다.
 *
 * [담당] 공통 기반
 */
@Getter
@Setter
@ToString
public class Review {

    private int reviewId;               // review_id      PK
    private String reviewContent;       // review_content 리뷰 내용 (NULL 가능)
    private int rating;                 // rating         별점 1~5
    private LocalDateTime reviewAt;     // review_at      작성 시간
    private int userId;                 // user_id        작성한 서포터
    private int projectId;              // project_id
}
