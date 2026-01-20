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

    /**
     * 월간 보고서 생성
     */
    public String generateMonthlyReport(String taskType, List<Map<String, Object>> tasks) {
        StringBuilder promptBuilder = new StringBuilder();
        
        java.time.LocalDate now = java.time.LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        String monthName;
        switch (month) {
            case 1: monthName = "1월"; break;
            case 2: monthName = "2월"; break;
            case 3: monthName = "3월"; break;
            case 4: monthName = "4월"; break;
            case 5: monthName = "5월"; break;
            case 6: monthName = "6월"; break;
            case 7: monthName = "7월"; break;
            case 8: monthName = "8월"; break;
            case 9: monthName = "9월"; break;
            case 10: monthName = "10월"; break;
            case 11: monthName = "11월"; break;
            case 12: monthName = "12월"; break;
            default: monthName = String.valueOf(month) + "월"; break;
        }
        
        promptBuilder.append(String.format("다음은 %s 과제들의 %d년 %s 활동 내역입니다.\n\n", taskType, year, monthName));
        
        int inputtedCount = 0;
        int notInputtedCount = 0;
        
        for (Map<String, Object> task : tasks) {
            String taskName = (String) task.get("taskName");
            String activityContent = (String) task.get("activityContent");
            
            if (activityContent != null && !activityContent.trim().isEmpty()) {
                promptBuilder.append(String.format("- **%s**\n  - %s\n\n", taskName, activityContent));
                inputtedCount++;
            } else {
                promptBuilder.append(String.format("- **%s**\n  - 활동내역 없음\n\n", taskName));
                notInputtedCount++;
            }
        }
        
        promptBuilder.append("위 정보를 바탕으로 전문적이고 체계적인 월간 보고서를 마크다운 형식으로 작성해주세요.\n\n");
        promptBuilder.append("## 보고서 작성 요구사항\n\n");
        promptBuilder.append("1. **보고서 구조**: 다음 섹션을 포함하여 작성해주세요.\n");
        promptBuilder.append("   - ## 1. 개요\n");
        promptBuilder.append("   - ## 2. 주요 활동 현황\n");
        promptBuilder.append("   - ## 3. 성과 및 결과\n");
        promptBuilder.append("   - ## 4. 향후 계획\n\n");
        
        promptBuilder.append("2. **작성 스타일**:\n");
        promptBuilder.append("   - 각 섹션은 명확한 제목(##)으로 구분\n");
        promptBuilder.append("   - 구체적이고 객관적인 서술\n");
        promptBuilder.append("   - 불필요한 인사말이나 서론 없이 핵심 내용만 작성\n");
        promptBuilder.append("   - 숫자나 통계가 있으면 포함 (예: 총 과제 수, 입력 완료 과제 수 등)\n\n");
        
        promptBuilder.append("3. **활동내역 처리**:\n");
        promptBuilder.append(String.format("   - 입력된 과제 (%d개): 구체적인 활동내역을 바탕으로 상세히 서술\n", inputtedCount));
        promptBuilder.append(String.format("   - 미입력 과제 (%d개): 해당 과제명을 명시하고, 미입력 사유나 향후 계획을 포함\n\n", notInputtedCount));
        
        promptBuilder.append("4. **형식**:\n");
        promptBuilder.append("   - 마크다운 문법 사용 (##, **, -, 등)\n");
        promptBuilder.append("   - 가독성을 위한 적절한 줄바꿈\n");
        promptBuilder.append("   - 각 섹션은 3-5문단 정도로 구성\n\n");
        
        promptBuilder.append("보고서를 작성해주세요.");
        
        return callAX4(promptBuilder.toString());
    }
}
