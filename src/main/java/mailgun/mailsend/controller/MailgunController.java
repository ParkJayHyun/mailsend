package mailgun.mailsend.controller;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.extern.slf4j.Slf4j;
import mailgun.mailsend.domain.Bounce;
import mailgun.mailsend.domain.CsvMail;
import mailgun.mailsend.domain.Target;
import mailgun.mailsend.domain.TargetType;
import mailgun.mailsend.dto.Form;
import mailgun.mailsend.service.TargetService;
import mailgun.mailsend.service.ThreadService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class MailgunController {

    static final int MAX_LIMIT = 3000;
    private final String CREATE_BOUNCE_LIST = "bounce_test.csv";

    @Resource
    TargetService targetService;

    @Resource
    ThreadService threadService;

    @ModelAttribute("targetTypes")
    public TargetType[] targetTypes() {
        return TargetType.values();
    }
    
    /**
     * list 조회
     * @param model
     * @return
     */
    @GetMapping
    public String list(Model model) {
        int bounceCount = targetService.allBounceList().size();
        int failCount = targetService.allFailList().size();
        int sendedCount = targetService.allSendedList().size();
        int allCount = targetService.findAll().size();
        int sendCount = targetService.allSendList().size();
        model.addAttribute("bounceCount", bounceCount);
        model.addAttribute("failCount", failCount);
        model.addAttribute("sendedCount", sendedCount);
        model.addAttribute("allCount", allCount);
        model.addAttribute("sendCount", sendCount);
        model.addAttribute("form",new Form());

        return "list";
    }


    /**
     * 메일 전송
     * @param form
     * @return
     */

    @PostMapping("/send")
    public String send(Form form) {
        log.info("########## Send start ##########");
        try {
            //Target 종류별 List 가져오기
            List<String> list = new ArrayList<>();
            if(TargetType.FAIL.equals(form.getTargetType())){
                list = getFailList(form.getMaxCount());
            }else if(TargetType.SEND.equals(form.getTargetType())){
                list = getSendList(form.getMaxCount());
            }
            log.info("SendList Size = {}", list.size());
            if (list.size() > 0) {
                //메일 전송하기
                for (int index = 0; index < list.size(); ) {
                    int limitSize = index + MAX_LIMIT;
                    if (limitSize > list.size()) {
                        limitSize = list.size();
                    }
                    List<String> sendList = list.subList(index, limitSize);
                    threadService.call(form,sendList);
                    index += MAX_LIMIT;
                }
            }
        } catch (Exception e) {
            log.error("Send Error = {}", e);
        }
        log.info("########## Send End ##########");
        return "redirect:";
    }

    private List<String> getSendList(int maxCount) {
        List<Target> sendList = targetService.getSendList(maxCount);
        return sendList.stream().map(Target::getEmail).collect(Collectors.toList());
    }

    private List<String> getFailList(int maxCount) {
        List<Target> sendList = targetService.getFailList(maxCount);
        return sendList.stream().map(Target::getEmail).collect(Collectors.toList());
    }

    @PostMapping("upload-csv-file")
    public String uploadCSVFile(@RequestParam("file") MultipartFile file,
                                @RequestParam String dataType ,
                                Model model) {
        log.info("dataType ={}", dataType);
        if (file.isEmpty()) {

        }else{
            try{
                Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
                List<CsvMail> list = new CsvToBeanBuilder(reader)
                        .withType(CsvMail.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .build()
                        .parse();

                log.info("list = {}",list);

            }catch (Exception e){
                log.error("upload-csv-file reader error = {}",e);
            }
        }

        return "redirect:";
    }


    /**
     * Mailgun Bounce List File Create
     * @param apiUrl
     * @param apiKey
     * @param model
     */

    @GetMapping("/create-bounce-list")
    public void createBounceByMailgun(@RequestParam String apiUrl,
                                      @RequestParam String apiKey,
                                      Model model) {
        List<Bounce> list = new ArrayList<>();
        //API 호출
        String url = apiUrl;
        String nextUrl = "";
        while(true){
            try {
                if (!nextUrl.isEmpty()) {
                    url = nextUrl;
                }
                HttpResponse<JsonNode> request = Unirest.get(url)
                        .basicAuth("api", apiKey)
                        .field("limit", "10000")
                        .asJson();
                JsonNode body = request.getBody();
                JSONObject object = body.getObject();
                //items -> 객체 변환
                Object items = object.get("items");
                Gson gons = new Gson();
                Type listType = new TypeToken<ArrayList<Bounce>>() {}.getType();
                List<Bounce> bounceList = gons.fromJson(items.toString(), listType);
                if (!bounceList.isEmpty()) {
                    list.addAll(bounceList);
                    //다음 호출할 페이지 정보 찾기
                    JSONObject paging = (JSONObject) object.get("paging");
                    nextUrl = (String) paging.get("next");
                } else {
                    break;
                }
            } catch (Exception e) {
                log.error("Mailgun Api call error = {}",e);
            }
        }

        //CSV 만들기
        if (!list.isEmpty()) {
            try (PrintWriter writer = new PrintWriter(new File(CREATE_BOUNCE_LIST))) {
                StringBuilder sb = new StringBuilder();
                sb.append("address");
                sb.append('\n');
                for (Bounce bounce : list) {
                    sb.append(bounce.getAddress());
                    sb.append('\n');
                }
                writer.write(sb.toString());
                writer.close();
                log.info("Create File Success!");
                System.out.println("done!");

            } catch (Exception e) {
                log.error("Create File Error = {}",e);
            }
        }
    }


    /*@PostConstruct
    public void inti() {
        for(int i = 1 ;i <=300000;i++){
            String email = "test"+i;
            targetService.save(email);
        }
    }*/
}
