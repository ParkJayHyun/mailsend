package mailgun.mailsend.service;

import mailgun.mailsend.domain.Target;

import java.util.List;

public interface TargetService {

    Target save(String email);
    Target findTargetByEmail(String email);
    List<Target> findAll();
    List<Target> allBounceList();
    List<Target> allSendedList();
    List<Target> allFailList();
    List<Target> allSendList();
    List<Target> getSendList(int maxCount);
    List<Target> getFailList(int maxCount);
    Target update(Target target);

}
