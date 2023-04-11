package hanghae99.rescuepets.publicpet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hanghae99.rescuepets.common.dto.CustomException;
import hanghae99.rescuepets.common.dto.ExceptionMessage;
import hanghae99.rescuepets.common.entity.*;
import hanghae99.rescuepets.publicpet.repository.PetInfoCompareRepository;
import hanghae99.rescuepets.publicpet.repository.PublicPetInfoRepository;
import hanghae99.rescuepets.scrap.repository.ScrapRepository;
import hanghae99.rescuepets.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import static hanghae99.rescuepets.common.dto.ExceptionMessage.*;
import static hanghae99.rescuepets.common.entity.PetStateEnum.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ApiScheduler {
    @Value("${public.api.key}")
    private String publicApiKey;
    private final PublicPetInfoRepository publicPetInfoRepository;
    private final PetInfoCompareRepository petInfoCompareRepository;
    @Value("${kakao.api.keys}")
    private String kakaoMapKey;
    private final SseService sseService;
    private final ScrapRepository scrapRepository;

    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    protected void apiSchedule() throws IOException {
        log.info("apiSchedule 동작");
        long startTime = System.currentTimeMillis();//시작 시간
        PetStateEnum[] state = {NOTICE, PROTECT, END};
        int stateNo = 0;
        int pageNo = 0;
        String size = "1000";
        while (stateNo < 3) {
            pageNo++;
            String apiUrl = createApiUrl(String.valueOf(pageNo), state[stateNo], size);
            JSONArray itemList = fetchDataFromApi(apiUrl);
            if (itemList == null || itemList.isEmpty()) {
                log.info("State: " + state[stateNo] + ",  pageNo: " + pageNo + "을 API로부터 데이터를 받아오지 못했습니다.-------------------------------------------------------------------------");
                stateNo++;
                pageNo = 0;
                continue;
            }
            compareData(itemList, state[stateNo]);
        }
        long endTime = System.currentTimeMillis(); //종료 시간
        long executionTime = endTime - startTime; //소요 시간 계산
        log.info("-------------------------Execution time: " + executionTime + "ms");
    }

    protected String createApiUrl(String pageNo, PetStateEnum state, String size) throws UnsupportedEncodingException {
        StringBuilder urlBuilder = new StringBuilder("http://apis.data.go.kr/1543061/abandonmentPublicSrvc/abandonmentPublic");
        urlBuilder.append("?" + URLEncoder.encode("serviceKey", "UTF-8") + publicApiKey);
        urlBuilder.append("&" + URLEncoder.encode("bgnde", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8")); /*유기날짜(검색 시작일) (YYYYMMDD)*/
        urlBuilder.append("&" + URLEncoder.encode("endde", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8")); /*유기날짜(검색 종료일) (YYYYMMDD)*/
        urlBuilder.append("&" + URLEncoder.encode("upkind", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8")); /*축종코드 (개 : 417000, 고양이 : 422400, 기타 : 429900)*/
        urlBuilder.append("&" + URLEncoder.encode("kind", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8")); /*품종코드 (품종 조회 OPEN API 참조)*/
        urlBuilder.append("&" + URLEncoder.encode("upr_cd", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8")); /*시도코드 (시도 조회 OPEN API 참조)*/
        urlBuilder.append("&" + URLEncoder.encode("org_cd", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8")); /*시군구코드 (시군구 조회 OPEN API 참조)*/
        urlBuilder.append("&" + URLEncoder.encode("care_reg_no", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8")); /*보호소번호 (보호소 조회 OPEN API 참조)*/
        if (state.equals(PROTECT)) {
            urlBuilder.append("&" + URLEncoder.encode("state", "UTF-8") + "=" + URLEncoder.encode("protect", "UTF-8"));
        } else if (state.equals(NOTICE)) {
            urlBuilder.append("&" + URLEncoder.encode("state", "UTF-8") + "=" + URLEncoder.encode("notice", "UTF-8"));
        } else {
            urlBuilder.append("&" + URLEncoder.encode("state", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8"));
        }

        urlBuilder.append("&" + URLEncoder.encode("pageNo", "UTF-8") + "=" + URLEncoder.encode(pageNo, "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("numOfRows", "UTF-8") + "=" + URLEncoder.encode(size, "UTF-8")); /*페이지당 보여줄 개수 (1,000 이하), 기본값 : 10*/
        urlBuilder.append("&" + URLEncoder.encode("_type", "UTF-8") + "=" + URLEncoder.encode("json", "UTF-8")); /*xml(기본값) 또는 json*/

        return urlBuilder.toString();
    }

    protected JSONArray fetchDataFromApi(String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        log.info("Response code: " + conn.getResponseCode());
        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            log.info(new BufferedReader(new InputStreamReader(conn.getErrorStream())) + "");
            throw new IOException(); //체크
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();
        // JSON 데이터 파싱 및 추출
        JSONObject jsonObject = new JSONObject(sb.toString());
        JSONObject response = jsonObject.getJSONObject("response");
        JSONObject body = response.getJSONObject("body");
        JSONObject items = body.optJSONObject("items");
        JSONArray itemList = null;
        if (!items.isEmpty()) {
            itemList = items.getJSONArray("item");
        }
        return itemList;
    }

    protected void compareData(JSONArray itemList, PetStateEnum state) {
        for (int i = 0; i < itemList.length(); i++) {
            JSONObject itemObject = itemList.getJSONObject(i);
            Optional<PetInfoByAPI> petInfoByAPIOptional = publicPetInfoRepository.findByDesertionNo(itemObject.optString("desertionNo"));
            List<String> compareDataList = new ArrayList<>();
            if (petInfoByAPIOptional.isEmpty()) {
                String careAddr = itemObject.optString("careAddr");
                PetInfoByAPI petInfo = buildPetInfo(itemObject, state);
                List<Double> latitudeandlongitude = getLatLng(careAddr);
                if (latitudeandlongitude != null) {
                    petInfo.location(latitudeandlongitude.get(0), latitudeandlongitude.get(1), true);
                } else {
                    String[] careAddrParts = careAddr.split(" ");
                    if (careAddrParts.length >= 1) {
                        careAddr = careAddrParts[0] + " " + careAddrParts[1];
                        latitudeandlongitude = getLatLng(careAddr);
                        petInfo.location(latitudeandlongitude.get(0), latitudeandlongitude.get(1), false);
                    }
                }
                publicPetInfoRepository.save(petInfo);


            } else {
                PetInfoByAPI petInfoByAPI = petInfoByAPIOptional.get();
                Field[] fields = petInfoByAPI.getClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    String name = field.getName();
                    Object value = null;
                    try {
                        value = field.get(petInfoByAPI);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    if (itemObject.has(name)) {
                        if (!value.equals(itemObject.optString(name))) {
                            compareDataList.add(name);
                        }
                    }
//                    log.info(field.getName() + ": " + value);
                }
                if (!petInfoByAPI.getPetStateEnum().equals(state)) {
                    if (state.equals(END) && itemObject.optString("processState").contains("종료")) { //json 요청 state가 ""일 때 getProcessState도 종료 상태라면 데이터베이스 수정 요청
                        compareDataList.add("state");
                    }
                    if ((state.equals(PROTECT) || state.equals(NOTICE)) && itemObject.optString("processState").contains("보호")) {
                        compareDataList.add("state");
                    }
                }
                if (!compareDataList.isEmpty()) {
                    String compareDataKey = String.join(", ", compareDataList);
                    PetInfoCompare entityPetInfo = buildPetInfoEntity(petInfoByAPI, compareDataKey);

                    petInfoCompareRepository.save(entityPetInfo);

                    PetInfoByAPI petInfoByUpdate = buildPetInfo(itemObject, state);

                    petInfoByAPI.update(petInfoByUpdate);
                    PetInfoCompare petInfoEntity = buildPetInfoApi(itemObject, state, compareDataKey);

                    petInfoCompareRepository.save(petInfoEntity);

                    publicPetInfoRepository.saveAndFlush(petInfoByAPI);
                    Optional<Scrap> petInfoByApiScrap = scrapRepository.findByPetInfoByAPI_DesertionNo(petInfoByAPI.getDesertionNo());
                    petInfoByApiScrap.ifPresent(scrap -> sseService.send(scrap.getMember(), NotificationType.UPDATE, "회원님이 스크랩한 게시물 정보가 업데이트 되었습니다."));
                }
            }
        }
    }

    //유지헤야할 DB
    protected PetInfoByAPI buildPetInfo(JSONObject itemObject, PetStateEnum state) {
        return PetInfoByAPI.builder()
                .desertionNo(itemObject.optString("desertionNo"))
                .filename(itemObject.optString("filename"))
                .happenDt(itemObject.optString("happenDt"))
                .happenPlace(itemObject.optString("happenPlace"))
                .kindCd(itemObject.optString("kindCd"))
                .colorCd(itemObject.optString("colorCd"))
                .age(itemObject.optString("age"))
                .weight(itemObject.optString("weight"))
                .noticeNo(itemObject.optString("noticeNo"))
                .noticeSdt(itemObject.optString("noticeSdt"))
                .noticeEdt(itemObject.optString("noticeEdt"))
                .popfile(itemObject.optString("popfile"))
                .processState(itemObject.optString("processState"))
                .sexCd(itemObject.optString("sexCd"))
                .neuterYn(itemObject.optString("neuterYn"))
                .specialMark(itemObject.optString("specialMark"))
                .careNm(itemObject.optString("careNm"))
                .careTel(itemObject.optString("careTel"))
                .careAddr(itemObject.optString("careAddr"))
                .orgNm(itemObject.optString("orgNm"))
                .chargeNm(itemObject.optString("chargeNm"))
                .officetel(itemObject.optString("officetel"))
                .petStateEnum(state)
                .build();
    }


    //비교 DB 확인용 (json 저장)/최종 삭제 예정
    protected PetInfoCompare buildPetInfoApi(JSONObject itemObject, PetStateEnum state, String compareDataKey) {
        return PetInfoCompare.builder()
                .desertionNo(itemObject.optString("desertionNo"))
                .filename(itemObject.optString("filename"))
                .happenDt(itemObject.optString("happenDt"))
                .happenPlace(itemObject.optString("happenPlace"))
                .kindCd(itemObject.optString("kindCd"))
                .colorCd(itemObject.optString("colorCd"))
                .age(itemObject.optString("age"))
                .weight(itemObject.optString("weight"))
                .noticeNo(itemObject.optString("noticeNo"))
                .noticeSdt(itemObject.optString("noticeSdt"))
                .noticeEdt(itemObject.optString("noticeEdt"))
                .popfile(itemObject.optString("popfile"))
                .processState(itemObject.optString("processState"))
                .sexCd(itemObject.optString("sexCd"))
                .neuterYn(itemObject.optString("neuterYn"))
                .specialMark(itemObject.optString("specialMark"))
                .careNm(itemObject.optString("careNm"))
                .careTel(itemObject.optString("careTel"))
                .careAddr(itemObject.optString("careAddr"))
                .orgNm(itemObject.optString("orgNm"))
                .chargeNm(itemObject.optString("chargeNm"))
                .officetel(itemObject.optString("officetel"))
                .state(state.getKorean())
                .objectType("Json")
                .compareDataKey(compareDataKey)
                .build();
    }

    //    비교 DB 확인용 /기존 DB 저장 (PetInfoByAPI 테이블) /최종 삭제 예정
    protected PetInfoCompare buildPetInfoEntity(PetInfoByAPI petInfoByAPI, String compareDataKey) {
        return PetInfoCompare.builder()
                .desertionNo(petInfoByAPI.getDesertionNo())
                .filename(petInfoByAPI.getFilename())
                .happenDt(petInfoByAPI.getHappenDt())
                .happenPlace(petInfoByAPI.getHappenPlace())
                .kindCd(petInfoByAPI.getKindCd())
                .colorCd(petInfoByAPI.getColorCd())
                .age(petInfoByAPI.getAge())
                .weight(petInfoByAPI.getWeight())
                .noticeNo(petInfoByAPI.getNoticeNo())
                .noticeSdt(petInfoByAPI.getNoticeSdt())
                .noticeEdt(petInfoByAPI.getNoticeEdt())
                .popfile(petInfoByAPI.getPopfile())
                .processState(petInfoByAPI.getProcessState())
                .sexCd(petInfoByAPI.getSexCd())
                .neuterYn(petInfoByAPI.getNeuterYn())
                .specialMark(petInfoByAPI.getSpecialMark())
                .careNm(petInfoByAPI.getCareNm())
                .careTel(petInfoByAPI.getCareTel())
                .careAddr(petInfoByAPI.getCareAddr())
                .orgNm(petInfoByAPI.getOrgNm())
                .chargeNm(petInfoByAPI.getChargeNm())
                .officetel(petInfoByAPI.getOfficetel())
                .state(petInfoByAPI.getPetStateEnum().getKorean())
                .objectType("Entity")
                .compareDataKey(compareDataKey)
                .build();
    }

    public List<Double> getLatLng(String address) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "KakaoAK " + kakaoMapKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("query", address);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(parameters, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://dapi.kakao.com/v2/local/search/address.json",
                entity,
                String.class
        );

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode documentsNode = rootNode.path("documents");
            if (documentsNode.isArray() && documentsNode.size() > 0) {
                JsonNode firstDocumentNode = documentsNode.get(0);
                double latitude = firstDocumentNode.path("y").asDouble();
                double longitude = firstDocumentNode.path("x").asDouble();

                List<Double> result = new ArrayList<>();
                result.add(latitude);
                result.add(longitude);
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
