package mailgun.mailsend.domain;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "target",indexes = @Index(name = "i_Target", columnList = "email"))
@Getter @Setter
public class Target {

    @Id
    @GeneratedValue
    @Column(name = "target_id")
    private Long id;
    private String email;
    private Boolean isSend;
    private Boolean isFail;
    private Boolean isBounce;

}
