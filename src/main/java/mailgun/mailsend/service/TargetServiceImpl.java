package mailgun.mailsend.service;

import mailgun.mailsend.domain.Target;
import mailgun.mailsend.repository.TargetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TargetServiceImpl implements TargetService{

    @Resource
    TargetRepository targetRepository;

    @Override
    @Transactional
    public Target save(String email) {
        Target target = new Target();
        target.setEmail(email);
        target.setIsBounce(false);
        target.setIsSend(false);
        target.setIsFail(false);
        return targetRepository.save(target);
    }

    @Override
    public Target findTargetByEmail(String email) {
        return targetRepository.findTargetByEmail(email);
    }

    @Override
    public List<Target> findAll() {
        return targetRepository.findAll();

    }

    @Override
    public List<Target> allBounceList() {
        return targetRepository.allBounceList();
    }

    @Override
    public List<Target> allSendedList() {
        return targetRepository.allSendedList();
    }

    @Override
    public List<Target> allFailList() {
        return targetRepository.allFailList();
    }

    @Override
    public List<Target> allSendList() {
        return targetRepository.allSendList();
    }

    @Override
    public List<Target> getSendList(int maxCount) {
        return targetRepository.getSendList(maxCount);
    }

    @Override
    public List<Target> getFailList(int maxCount) {
        return targetRepository.getFailList(maxCount);
    }

    @Override
    @Transactional
    public Target update(Target target) {
        return targetRepository.save(target);
    }

    @Override
    public int findAllCount() {
        return targetRepository.findAllCount();
    }

    @Override
    public int allBounceCount() {
        return targetRepository.allBounceCount();
    }

    @Override
    public int allSendedCount() {
        return targetRepository.allSendedCount();
    }

    @Override
    public int allFailCount() {
        return targetRepository.allFailCount();
    }

    @Override
    public int allSendCount() {
        return targetRepository.allSendCount();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
