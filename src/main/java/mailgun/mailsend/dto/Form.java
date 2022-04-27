package mailgun.mailsend.dto;

import lombok.Getter;
import lombok.Setter;
import mailgun.mailsend.domain.TargetType;

@Setter
@Getter
public class Form {

    private String apiUrl;
    private String apiKey;
    private String templateName;
    private int maxCount;
    private TargetType targetType;

}
