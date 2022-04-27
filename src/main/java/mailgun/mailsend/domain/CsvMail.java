package mailgun.mailsend.domain;

import com.opencsv.bean.CsvBindByName;

public class CsvMail {

    @CsvBindByName
    private String email;

    public CsvMail(String email) {
        this.email = email;
    }
}
