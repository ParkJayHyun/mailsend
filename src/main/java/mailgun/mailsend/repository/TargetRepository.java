package mailgun.mailsend.repository;

import mailgun.mailsend.domain.Target;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class TargetRepository {

    @PersistenceContext
    EntityManager em;

    /**
     * Target 저장
     * @param target
     */
    public Target save(Target target) {
        if(target.getId() == null){
            em.persist(target);
        }else {
            em.merge(target);
        }
        return target;
    }

    /**
     * Target 조회
     * @param email
     * @return
     */
    public Target findTargetByEmail(String email) {
        List<Target> email1 = em.createQuery("select m from Target m where m.email =:email", Target.class)
                .setParameter("email", email)
                .getResultList();
        return email1.size() > 0 ?  email1.get(0): null ;

    }

    /**
     * ALl Target 조회
     * @return
     */
    public List<Target> findAll(){
        return em.createQuery("select m from Target m", Target.class)
                .getResultList();
    }

    /**
     * Bounce 대상 조회
     * #####조건#####
     * isBounce = true
     * @return
     */
    public List<Target> allBounceList() {
        return em.createQuery("select m from Target m where m.isBounce = true ", Target.class)
                .getResultList();
    }

    /**
     * Sended Target 조회
     * #####조건#####
     * isBounce = false
     * isSend = true
     * isFail = false
     * @return
     */
    public List<Target> allSendedList() {
        return em.createQuery("select m from Target m where m.isBounce = false and m.isFail = false and m.isSend = true ", Target.class)
                .getResultList();
    }

    /**
     * Fail Target 조회
     * #####조건#####
     * isBounce = false
     * isSend = false
     * isFail = true
     * @return
     */
    public List<Target> allFailList() {
        return em.createQuery("select m from Target m where m.isBounce = false and m.isFail = true and m.isSend = false ", Target.class)
                .getResultList();
    }

    /**
     * 요청한 maxCount 만큼 메일 대상 조회
     * #####조건#####
     * isBounce = false
     * isFail = false
     * isSend = false
     * @return
     */
    public List<Target> getSendList(int maxCount) {
        return em.createQuery("select m from Target m where m.isBounce = false and m.isFail = false and m.isSend = false ", Target.class)
                .setMaxResults(maxCount)
                .getResultList();
    }

    /**
     * 메일 발송 예정 대상 조회
     * @return
     */
    public List<Target> allSendList() {
        return em.createQuery("select m from Target m where m.isBounce = false and m.isFail = false and m.isSend = false ", Target.class)
                .getResultList();
    }

    /**
     * Fail건에 대해서 Retry
     * isBounce = false
     * isFail = true
     * isSend = false
     * @param maxCount
     * @return
     */
    public List<Target> getFailList(int maxCount) {
        return em.createQuery("select m from Target m where m.isBounce = false and m.isFail = true and m.isSend = false ", Target.class)
                .setMaxResults(maxCount)
                .getResultList();
    }
}
