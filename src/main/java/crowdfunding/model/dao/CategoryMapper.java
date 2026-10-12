package crowdfunding.model.dao;

import crowdfunding.model.domain.Category;

import java.util.List;

/** category 테이블 DAO — SQL 은 resources/mapper/CategoryMapper.xml */
public interface CategoryMapper {

    List<Category> findAll();

    Category findById(int categoryId);

    int insertCategory(Category category);
}
