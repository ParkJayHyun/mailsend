package mailgun.mailsend.domain;

public enum TargetType {

    SEND("발송예정(IS_SEND=FALSE)"),FAIL("발송 실패 Retry(IS_FAIL=TRUE)");

    private final String description;

    TargetType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
