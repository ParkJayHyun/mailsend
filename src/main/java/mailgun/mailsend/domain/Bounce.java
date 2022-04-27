package mailgun.mailsend.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Bounce {
    private String address;
    private String code;
    private String error;
    private String created_at;
    private String MessageHash;
}
