package mailgun.mailsend.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.extern.slf4j.Slf4j;
import mailgun.mailsend.domain.Bounce;
import mailgun.mailsend.domain.CsvMail;
import mailgun.mailsend.domain.Target;
import mailgun.mailsend.domain.TargetType;
import mailgun.mailsend.dto.Form;
import mailgun.mailsend.service.TargetService;
import mailgun.mailsend.service.ThreadService;
import mailgun.mailsend.thread.ThreadPoolInit;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class MailgunController {

    @Resource
    TargetService targetService;

    @Resource
    ThreadService threadService;

    @Resource
    ThreadPoolInit threadPoolInit;

    @ModelAttribute("targetTypes")
    public TargetType[] targetTypes() {
        return TargetType.values();
    }

    /**
     * list 조회
     *
     * @param model
     * @return
     */
    @GetMapping
    public String list(Model model) {
        int allCount = targetService.findAllCount();
        int sendCount = targetService.allSendCount();
        int sendedCount = targetService.allSendedCount();
        int bounceCount = targetService.allBounceCount();
        int failCount = targetService.allFailCount();

        model.addAttribute("bounceCount", bounceCount);
        model.addAttribute("failCount", failCount);
        model.addAttribute("sendedCount", sendedCount);
        model.addAttribute("allCount", allCount);
        model.addAttribute("sendCount", sendCount);
        model.addAttribute("form", new Form());
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) threadPoolInit.setExecutor();
        model.addAttribute("threadActiveCount", executor.getActiveCount());
        log.info("Thread ActiveCount = {}", executor.getActiveCount());
        return "list";
    }


    /**
     * 메일 전송
     *
     * @param form
     * @return
     */

    @PostMapping("/send")
    public String send(Form form) {
        log.info("########## Send start ##########");
        try {
            ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) threadPoolInit.setExecutor();
            int maxPoolSize = executor.getMaxPoolSize();
            //Target 종류별 List 가져오기
            List<String> list = new ArrayList<>();
            if (TargetType.FAIL.equals(form.getTargetType())) {
                list = getFailList(form.getMaxCount());
            } else if (TargetType.SEND.equals(form.getTargetType())) {
                list = getSendList(form.getMaxCount());
            }
            log.info("SendList Size = {}", list.size());
            if (list.size() > 0) {
                int maxLimit = (int) Math.ceil(((double) list.size() / (double) maxPoolSize));
                log.info("SendList MaxLimit = {}", maxLimit);
                //메일 전송하기
                for (int index = 0; index < list.size(); ) {
                    int limitSize = index + maxLimit;
                    if (limitSize > list.size()) {
                        limitSize = list.size();
                    }
                    List<String> sendList = list.subList(index, limitSize);
                    threadService.call(form, sendList);
                    index += maxLimit;
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
                                @RequestParam String dataType,
                                Model model) {
        log.info("####### dataType ={} Start #######", dataType);
        if (!file.isEmpty()) {
            try {
                Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "MS949"));
                List<CsvMail> filelist = new CsvToBeanBuilder(reader)
                        .withType(CsvMail.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .build()
                        .parse();

                if (filelist.size() > 0) {
                    List<String> list = filelist.stream().map(CsvMail::getEmail).collect(Collectors.toList());
                    if (list.size() > 0) {
                        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) threadPoolInit.setExecutor();
                        int maxPoolSize = executor.getMaxPoolSize();
                        int maxLimit = (int) Math.ceil(((double) list.size() / (double) maxPoolSize));
                        //int maxLimit = (int) Math.ceil(((double) list.size()/(double) maxPoolSize)/1000)*1000;
                        log.info("upload-csv-file MaxLimit = {}, list = {}", maxLimit, list.size());

                        //Upload File Create
                        for (int index = 0; index < list.size(); ) {
                            int limitSize = index + maxLimit;
                            if (limitSize > list.size()) {
                                limitSize = list.size();
                            }
                            List<String> sendList = list.subList(index, limitSize);
                            if ("create_not_korea_word".equals(dataType)) {
                                List<String> filterList = new ArrayList<>();
                                for (String email : sendList) {
                                    if (!isKoreaWord(email)) {
                                        filterList.add(email);
                                    } else {
                                        log.info("email contain korea word = {}", email);
                                    }
                                }
                                sendList = filterList;
                                threadService.createUser(sendList);
                            }
                            if ("create".equals(dataType)) {
                                threadService.createUser(sendList);
                            } else if ("bounce".equals(dataType)) {
                                threadService.bounceUser(sendList);
                            }
                            index += maxLimit;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("upload-csv-file reader error = {}", e);
            }
        }
        log.info("####### dataType ={} End #######", dataType);
        return "redirect:";
    }

    @PostMapping("csv-file-filter-download")
    public ResponseEntity<byte[]> downloadCSVtoCSVFilter(@RequestParam("file") MultipartFile file,
                                                         Model model) {
        log.info("####### downloadCSVtoCSVFilter Start #######");
        if (!file.isEmpty()) {
            try {
                Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "MS949"));
                List<CsvMail> filelist = new CsvToBeanBuilder(reader)
                        .withType(CsvMail.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .build()
                        .parse();

                if (filelist.size() > 0) {
                    List<String> list = filelist.stream().map(CsvMail::getEmail).collect(Collectors.toList());
                    if (list.size() > 0) {
                        //filter 적용
                        List<String> filterList = new ArrayList<>();
                        for (String email : list) {
                            if (!isKoreaWord(email)) {
                                filterList.add(email);
                            } else {
                                log.info("email contain korea word = {}", email);
                            }
                        }
                        //filter된 list csvFile 생성
                        if(filterList.size() > 0){
                            String fileName = String.format("%s_filter_%s.csv", file.getOriginalFilename(),new SimpleDateFormat("yyyyMMdd").format(new Date()));
                            try {
                                HttpHeaders headers = new HttpHeaders();
                                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                                headers.set("Content-Disposition", "attachment; filename=" + fileName);
                                ByteArrayOutputStream os = new ByteArrayOutputStream();
                                StringBuilder sb = new StringBuilder();
                                sb.append("email");
                                sb.append('\n');
                                for (String filterEmail : filterList) {
                                    sb.append(filterEmail);
                                    sb.append('\n');
                                }
                                os.write(sb.toString().getBytes());
                                os.close();
                                log.info("Create CSV Filter File Success!");
                                return new ResponseEntity<>(os.toByteArray(), headers, HttpStatus.OK);
                            } catch (Exception e) {
                                log.error("Create File Error = {}", e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("upload-csv-file reader error = {}", e);
            }
        }
        log.info("####### downloadCSVtoCSVFilter End #######");
        return null;
    }


    /**
     * Mailgun Bounce List File Create
     */
    @GetMapping("/create-bounce-list")
    public ResponseEntity<byte[]> createBounceByMailgun(Form form) {
        List<Bounce> list = new ArrayList<>();
        //API 호출
        String url = form.getApiUrl();
        String nextUrl = "";
        while (true) {
            try {
                if (!nextUrl.isEmpty()) {
                    url = nextUrl;
                }
                HttpResponse<JsonNode> request = Unirest.get(url)
                        .basicAuth("api", form.getApiKey())
                        .field("limit", "10000")
                        .asJson();
                JsonNode body = request.getBody();
                JSONObject object = body.getObject();
                //items -> 객체 변환
                Object items = object.get("items");
                Gson gons = new Gson();
                Type listType = new TypeToken<ArrayList<Bounce>>() {
                }.getType();
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
                log.error("Mailgun Api call error = {}", e);
            }
        }

        //CSV 만들기
        if (!list.isEmpty()) {
            String fileName = String.format("bounce_list_%s.csv", new SimpleDateFormat("yyyyMMdd").format(new Date()));
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.set("Content-Disposition", "attachment; filename=" + fileName);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                StringBuilder sb = new StringBuilder();
                sb.append("email");
                sb.append('\n');
                for (Bounce bounce : list) {
                    sb.append(bounce.getAddress());
                    sb.append('\n');
                }
                os.write(sb.toString().getBytes());
                os.close();
                log.info("Create File Success!");
                return new ResponseEntity<>(os.toByteArray(), headers, HttpStatus.OK);
            } catch (Exception e) {
                log.error("Create File Error = {}", e);
            }
        }
        return null;
    }

    public Boolean isKoreaWord(String email) {
        boolean result = false;
        if(email != null){
            if (email.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
                // 한글이 포함된 문자열
                result = true;
            }
        }
        return result;
    }
}
