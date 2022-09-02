package mailgun.mailsend.service;

import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import lombok.extern.slf4j.Slf4j;
import mailgun.mailsend.domain.Target;
import mailgun.mailsend.domain.TargetType;
import mailgun.mailsend.dto.Form;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.annotation.Resource;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Service
@Slf4j
public class ThreadService {

    static String sender = "나이키닷컴 <postmaster@nike.co.kr>";
    //static String title ="[NIKE.COM] 개인정보 이용 내역 통지 안내";
    //static String title ="[나이키] 개인 정보 국외 이전 동의 요청 안내";
    //static String title ="[나이키] 휴면계정 삭제 예정안내";
    static String title ="[나이키] MVP 회원 등급 안내";
    //static String title ="[나이키] 개인 정보 국외 이전 및 제 3자 제공 동의 요청";


    @Resource
    TargetService targetService;

    @Async("executor")
    public void call(Form form, List<String> sendList) {
        String apiKey = form.getApiKey();
        String apiUrl = form.getApiUrl();
        TargetType targetType = form.getTargetType();
        String templateName = form.getTemplateName();

        for(String email : sendList){
            try{
                Target target = targetService.findTargetByEmail(email);
                if(target != null){
                    if(target.getIsSend() == false){
                        try{
                            ClientResponse clientResponse = send(target.getEmail(),apiKey,apiUrl,templateName);
                            if (clientResponse.getStatusInfo().getStatusCode() == 200) {
                                try{
                                    target.setIsFail(false);
                                    target.setIsSend(true);
                                    targetService.update(target);
                                    //log.info("Success Send mail = {}", target.getEmail());
                                }catch (Exception e){
                                    log.error("IsSend Update Error = {}, mail = {}",e,target.getEmail());
                                }
                            }else {
                                target.setIsFail(true);
                                targetService.update(target);
                                log.info("clientResponse.getStatusInfo().getStatusCode() = {}, mail = {}",clientResponse.getStatusInfo().getStatusCode(),target.getEmail());
                            }
                        }catch (Exception e) {
                            log.error("Http Error = {}, mail = {}",e,target.getEmail());
                        }
                    }else {
                        log.info("Already send mail = {}",target.getEmail());
                    }
                }else{
                    log.info("Target Data is not exist = {}" , target.getEmail());
                }
            }catch (Exception e) {
                log.error("Service Error = {}", e);
            }
        }
    }

    public ClientResponse send(String email, String apiKey, String apiUrl, String templateName) {
        Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter("api", apiKey));
        WebResource webResource = client.resource(apiUrl);
        MultivaluedMapImpl formData = new MultivaluedMapImpl();
        formData.add("from", sender);
        formData.add("to", email);
        formData.add("subject", title);
        //formData.add("template", "customer_info");
        formData.add("template",templateName);
        return webResource.type(MediaType.APPLICATION_FORM_URLENCODED).
                post(ClientResponse.class, formData);
    }
    @Async("executor")
    public Target createUser(String email) {
        return targetService.save(email);
    }

    @Async("executor")
    public void createUser(List<String> createList) {
        for(String email : createList){
            if(!isKoreaWord(email)){
                try {
                    targetService.save(email);
                }catch (Exception e){
                    log.error("upload file create user error = {}",e);
                }
            }else{
                log.info("한글이 포함된 email = {}", email);
            }
        }
    }

    @Async("executor")
    public void bounceUser(List<String> bounceList) {
        for(String email : bounceList){
            try {
                Target bounceTarget = targetService.findTargetByEmail(email);
                if(bounceTarget != null){
                    bounceTarget.setIsBounce(true);
                    targetService.update(bounceTarget);
                }
            }catch (Exception e){
                log.error("upload file create user error = {}",e);
            }
        }
    }

    public static Boolean isKoreaWord(String email) {
        boolean result = false;
        if(email.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
            // 한글이 포함된 문자열
            result = true;
        }
        return result;
    }

    public static void main(String[] args) {
        String email = "계동규dounner52@naver.com";
        System.out.println("isKoreaWord = " + isKoreaWord(email));
    }
}
