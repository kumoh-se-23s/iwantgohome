package crowdfunding.model.enums;

/** 사용자 권한 — user.role ENUM('supporter','maker','admin') */
public enum Role implements CodeEnum {
    SUPPORTER("supporter", "서포터"),
    MAKER("maker", "메이커"),
    ADMIN("admin", "관리자");

    private final String code;
    private final String label;

    Role(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @Override public String getCode() { return code; }
    @Override public String getLabel() { return label; }
}
