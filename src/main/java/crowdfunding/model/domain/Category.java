package crowdfunding.model.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * category 테이블 한 행.
 *
 * [담당] 공통 기반
 */
@Getter
@Setter
@ToString
public class Category {

    private int categoryId;
    private String categoryName;
}
