package crowdfunding.model.dao;

import org.apache.ibatis.annotations.Param;

/** 검사용 사전 데이터(initDB) 전용 — 테이블 비우기 */
public interface DataInitMapper {

    int disableForeignKeyChecks();

    int enableForeignKeyChecks();

    /** tableName 은 DataInitializer 안의 고정 문자열만 넘긴다 (외부 입력 금지) */
    int truncateTable(@Param("tableName") String tableName);
}
