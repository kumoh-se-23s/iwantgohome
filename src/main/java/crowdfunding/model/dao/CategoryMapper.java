package crowdfunding.model.dao;

import crowdfunding.model.domain.Category;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** category 테이블 SQL (resources/mapper/CategoryMapper.xml) */
public interface CategoryMapper {

    boolean exists(@Param("categoryId") int categoryId);

    List<Category> findAll();
}
