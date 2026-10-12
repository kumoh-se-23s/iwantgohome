package crowdfunding.model.dao;

import crowdfunding.model.enums.CodeEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler: DB ENUM 문자열 ↔ {@link CodeEnum} 을 구현한 자바 enum 변환.
 * <p>
 * MyBatis 기본 EnumTypeHandler 는 enum 이름(IN_PROGRESS)을 그대로 저장하지만
 * 우리 DB 는 소문자 코드('in_progress')를 쓰므로 직접 변환기를 만든다.
 * mybatis-config.xml 의 &lt;typeHandlers&gt; 에 enum 별로 등록되어 있다.
 */
public class CodeEnumTypeHandler<E extends Enum<E> & CodeEnum> extends BaseTypeHandler<E> {

    private final Class<E> type;

    /** MyBatis 가 등록된 javaType(예: ProjectStatus.class)을 넣어 생성해 준다. */
    public CodeEnumTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("enum type 이 필요합니다.");
        }
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getCode());          // 자바 → DB : 코드 문자열 저장
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toEnum(rs.getString(columnName));       // DB → 자바
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toEnum(rs.getString(columnIndex));
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toEnum(cs.getString(columnIndex));
    }

    private E toEnum(String code) {
        if (code == null) {
            return null;                                // project_status 는 승인 전 NULL
        }
        for (E constant : type.getEnumConstants()) {
            if (constant.getCode().equals(code)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(type.getSimpleName() + " 에 없는 DB 값: " + code);
    }
}
