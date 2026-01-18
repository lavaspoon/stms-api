package devlava.stmsapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private static final String AI_API_URL = "https://guest-api.sktax.chat/v1/chat/completions";
    private static final String AI_API_KEY = "sktax-XyeKFrq67ZjS4EpsDlrHHXV8it";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * A.X 4.0 AI API 호출
     */
    public String callAX4(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(AI_API_KEY);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "ax4");
            requestBody.put("messages", List.of(message));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("AI API 호출: {}", prompt);
            ResponseEntity<String> response = restTemplate.exchange(
                    AI_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").get(0).path("message").path("content").asText();
                log.info("AI API 응답: {}", content);
                return content;
            } else {
                log.error("AI API 호출 실패: {}", response.getStatusCode());
                throw new RuntimeException("AI API 호출 실패");
            }
        } catch (Exception e) {
            log.error("AI API 호출 중 오류 발생", e);
            throw new RuntimeException("AI API 호출 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 맞춤법 검사
     */
    public String checkSpelling(String text) {
        String prompt = String.format(
                "다음 텍스트의 맞춤법과 띄어쓰기를 검사하고 교정해주세요. 교정된 텍스트만 출력하고 설명은 하지 마세요.\n\n%s",
                text
        );
        return callAX4(prompt);
    }

    /**
     * 활동내역 추천
     */
    public String recommendActivity(String taskName, String previousActivities) {
        String prompt = String.format(
                "과제명: \"%s\"\n\n이전 활동내역:\n%s\n\n위 과제의 이번 달 활동내역을 작성해주세요. 간결하고 구체적으로 3-5줄 정도로 작성해주세요. 불필요한 인사말이나 설명 없이 활동내역만 작성해주세요.",
                taskName,
                previousActivities != null && !previousActivities.isEmpty() ? previousActivities : "없음"
        );
        return callAX4(prompt);
    }

    /**
     * 문맥 교정
     */
    public String improveContext(String text) {
        String prompt = String.format(
                "다음 활동내역의 문맥과 표현을 더 명확하고 전문적으로 개선해주세요. 개선된 텍스트만 출력하고 설명은 하지 마세요.\n\n%s",
                text
        );
        return callAX4(prompt);
    }
}
