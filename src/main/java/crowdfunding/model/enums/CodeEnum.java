package crowdfunding.model.enums;

/**
 * DB 의 ENUM 컬럼 값(문자열 코드)과 1:1 로 대응하는 자바 enum 이 구현하는 인터페이스.
 * <p>
 * 예) ProjectStatus.IN_PROGRESS ↔ DB 값 'in_progress'
 * MyBatis 의 {@link crowdfunding.model.dao.CodeEnumTypeHandler} 가 이 코드를 이용해 자동 변환한다.
 */
public interface CodeEnum {

    /** DB 에 저장되는 값 (예: "in_progress") */
    String getCode();

    /** 화면에 보여줄 한글 이름 (예: "펀딩 진행 중") */
    String getLabel();
}
