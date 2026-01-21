package devlava.stmsapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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

    /**
     * 보고서 생성 (형식별)
     */
    public String generateReport(String taskType, List<Map<String, Object>> tasks, String reportType, String format) {
        if (format == null || format.equals("markdown")) {
            // 기본 마크다운 형식
            if (reportType.equals("monthly")) {
                return generateMonthlyReport(taskType, tasks);
            } else {
                return generateComprehensiveReport(taskType, tasks);
            }
        } else if (format.equals("html")) {
            // HTML 뉴스클립 스타일
            return generateHTMLReport(taskType, tasks, reportType);
        } else {
            // 마크다운 기본
            if (reportType.equals("monthly")) {
                return generateMonthlyReport(taskType, tasks);
            } else {
                return generateComprehensiveReport(taskType, tasks);
            }
        }
    }

    /**
     * HTML 뉴스클립 스타일 보고서 생성
     */
    private String generateHTMLReport(String taskType, List<Map<String, Object>> tasks, String reportType) {
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
        
        if (reportType.equals("monthly")) {
            promptBuilder.append(String.format("다음은 %s 과제들의 %d년 %s 활동 내역입니다.\n\n", taskType, year, monthName));
        } else {
            promptBuilder.append(String.format("다음은 %s 과제들의 지금까지의 모든 활동 내역입니다.\n\n", taskType));
        }
        
        int inputtedCount = 0;
        
        for (Map<String, Object> task : tasks) {
            String taskName = (String) task.get("taskName");
            if (reportType.equals("monthly")) {
                String activityContent = (String) task.get("activityContent");
                if (activityContent != null && !activityContent.trim().isEmpty()) {
                    promptBuilder.append(String.format("- **%s**: %s\n\n", taskName, activityContent));
                    inputtedCount++;
                }
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> activities = (List<Map<String, Object>>) task.get("activities");
                if (activities != null && !activities.isEmpty()) {
                    promptBuilder.append(String.format("### %s\n\n", taskName));
                    for (Map<String, Object> activity : activities) {
                        Integer activityYear = (Integer) activity.get("activityYear");
                        Integer activityMonth = (Integer) activity.get("activityMonth");
                        String activityContent = (String) activity.get("activityContent");
                        if (activityContent != null && !activityContent.trim().isEmpty()) {
                            promptBuilder.append(String.format("- **%d년 %d월**: %s\n\n", activityYear, activityMonth, activityContent));
                            inputtedCount++;
                        }
                    }
                }
            }
        }
        
        promptBuilder.append("\n\n위 활동 내역을 바탕으로 아래 제공된 HTML 구조를 사용하여 뉴스클립 스타일 보고서를 작성해주세요.\n\n");
        promptBuilder.append("## 중요 지침:\n\n");
        promptBuilder.append("1. 아래 제공된 HTML 구조를 정확히 그대로 유지하세요.\n");
        promptBuilder.append("2. HTML 구조의 클래스명, 태그명, CSS 링크를 절대 변경하지 마세요.\n");
        promptBuilder.append("3. 플레이스홀더만 실제 데이터로 교체하세요:\n");
        promptBuilder.append("   - {{REPORT_TITLE}}: 보고서 제목 (예: 'OI 과제 월간 보고서', '중점추진과제 종합 보고서')\n");
        promptBuilder.append("   - {{REPORT_DATE}}: 보고서 작성일 (예: '2025년 1월', '2025년 1월 15일')\n");
        promptBuilder.append("   - {{REPORT_CONTENT}}: 보고서 본문 내용 (HTML 태그 포함)\n");
        promptBuilder.append("4. {{REPORT_CONTENT}} 부분에는 다음 구조로 HTML을 작성하세요:\n");
        promptBuilder.append("   <div class=\"news-section\">\n");
        promptBuilder.append("       <div class=\"section-title\">1. 개요</div>\n");
        promptBuilder.append("       <div class=\"section-content\">...내용...</div>\n");
        promptBuilder.append("   </div>\n");
        promptBuilder.append("   <div class=\"news-section\">\n");
        promptBuilder.append("       <div class=\"section-title\">2. 주요 활동 현황</div>\n");
        promptBuilder.append("       <div class=\"section-content\">...내용...</div>\n");
        promptBuilder.append("   </div>\n");
        promptBuilder.append("   <div class=\"news-section\">\n");
        promptBuilder.append("       <div class=\"section-title\">3. 성과 및 결과</div>\n");
        promptBuilder.append("       <div class=\"section-content\">...내용...</div>\n");
        promptBuilder.append("   </div>\n");
        promptBuilder.append("   <div class=\"news-section\">\n");
        promptBuilder.append("       <div class=\"section-title\">4. 향후 계획</div>\n");
        promptBuilder.append("       <div class=\"section-content\">...내용...</div>\n");
        promptBuilder.append("   </div>\n");
        promptBuilder.append("5. 사용 가능한 CSS 클래스:\n");
        promptBuilder.append("   - .news-section: 섹션 컨테이너\n");
        promptBuilder.append("   - .section-title: 섹션 제목\n");
        promptBuilder.append("   - .section-content: 섹션 본문\n");
        promptBuilder.append("   - .highlight-card: 강조 카드\n");
        promptBuilder.append("   - .stats-grid: 통계 그리드\n");
        promptBuilder.append("   - .stat-card: 통계 카드\n");
        promptBuilder.append("   - .task-item: 과제 항목\n");
        promptBuilder.append("   - .task-name: 과제명\n");
        promptBuilder.append("   - .task-content: 과제 내용\n");
        promptBuilder.append("6. CSS는 별도 파일에 저장되어 있으므로, HTML 구조만 작성하고 CSS 링크는 그대로 유지하세요.\n");
        promptBuilder.append("7. 플레이스홀더를 교체한 완전한 HTML 문서만 반환하세요. 추가 설명이나 텍스트 없이 순수 HTML 코드만 반환하세요.\n\n");
        promptBuilder.append("## 제공된 HTML 구조:\n\n");
        promptBuilder.append("```html\n");
        promptBuilder.append(ReportTemplate.HTML_NEWS_CLIP_TEMPLATE);
        promptBuilder.append("\n```\n\n");
        promptBuilder.append("위 HTML 구조를 유지하면서 플레이스홀더를 실제 데이터로 교체한 완전한 HTML 문서를 작성해주세요.");
        
        String aiResponse = callAX4(promptBuilder.toString());
        
        // AI 응답에서 HTML 코드 블록 추출
        String htmlContent = extractHTMLFromResponse(aiResponse);
        
        // CSS를 인라인으로 포함시키기
        return injectInlineCSS(htmlContent);
    }

    /**
     * AI 응답에서 HTML 코드 추출
     * 정확히 HTML 문서만 추출하도록 정규식 사용
     */
    private String extractHTMLFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }

        // 1. ```html 코드 블록에서 추출 (가장 정확)
        Pattern htmlCodeBlockPattern = Pattern.compile("```html\\s*\\n?(.*?)```", Pattern.DOTALL);
        Matcher htmlCodeMatcher = htmlCodeBlockPattern.matcher(response);
        if (htmlCodeMatcher.find()) {
            String extracted = htmlCodeMatcher.group(1).trim();
            if (extracted.contains("<!DOCTYPE") || extracted.contains("<html")) {
                return extracted;
            }
        }

        // 2. ``` 코드 블록에서 HTML 추출
        Pattern codeBlockPattern = Pattern.compile("```\\s*\\n?(.*?)```", Pattern.DOTALL);
        Matcher codeMatcher = codeBlockPattern.matcher(response);
        while (codeMatcher.find()) {
            String code = codeMatcher.group(1).trim();
            if (code.contains("<!DOCTYPE") || code.contains("<html")) {
                return code;
            }
        }

        // 3. <!DOCTYPE html> 또는 <html>로 시작하는 완전한 HTML 문서 추출
        Pattern htmlDocumentPattern = Pattern.compile("<!DOCTYPE\\s+html[^>]*>.*?</html>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher htmlDocMatcher = htmlDocumentPattern.matcher(response);
        if (htmlDocMatcher.find()) {
            return htmlDocMatcher.group(0).trim();
        }

        // 4. <html>로 시작하는 HTML 문서 추출
        Pattern htmlTagPattern = Pattern.compile("<html[^>]*>.*?</html>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher htmlTagMatcher = htmlTagPattern.matcher(response);
        if (htmlTagMatcher.find()) {
            return htmlTagMatcher.group(0).trim();
        }

        // 5. <!DOCTYPE로 시작하는 부분부터 추출 (마지막 </html>까지)
        int doctypeStart = response.indexOf("<!DOCTYPE");
        if (doctypeStart >= 0) {
            int htmlEnd = response.lastIndexOf("</html>");
            if (htmlEnd > doctypeStart) {
                return response.substring(doctypeStart, htmlEnd + 7).trim();
            }
            // </html>이 없으면 <html> 태그를 찾아서 추출
            int htmlTagStart = response.indexOf("<html", doctypeStart);
            if (htmlTagStart >= 0) {
                int htmlTagEnd = response.lastIndexOf("</html>");
                if (htmlTagEnd > htmlTagStart) {
                    return response.substring(doctypeStart, htmlTagEnd + 7).trim();
                }
            }
        }

        // 6. <html>로 시작하는 부분부터 추출
        int htmlStart = response.indexOf("<html");
        if (htmlStart >= 0) {
            int htmlEnd = response.lastIndexOf("</html>");
            if (htmlEnd > htmlStart) {
                return response.substring(htmlStart, htmlEnd + 7).trim();
            }
        }

        // 7. 마지막 방법: <!DOCTYPE 또는 <html로 시작하는 부분만 반환 (안전하지 않지만 시도)
        if (response.contains("<!DOCTYPE") || response.contains("<html")) {
            int start = response.indexOf("<!DOCTYPE");
            if (start == -1) {
                start = response.indexOf("<html");
            }
            if (start >= 0) {
                // 최대한 </html>까지 찾기
                String remaining = response.substring(start);
                int end = remaining.indexOf("</html>");
                if (end > 0) {
                    return remaining.substring(0, end + 7).trim();
                }
                return remaining.trim();
            }
        }

        // HTML을 찾지 못한 경우 빈 문자열 반환
        log.warn("HTML 문서를 추출할 수 없습니다. 응답: {}", response.length() > 200 ? response.substring(0, 200) + "..." : response);
        return "";
    }

    /**
     * HTML에 CSS를 인라인으로 주입
     */
    private String injectInlineCSS(String html) {
        if (html == null || html.trim().isEmpty()) {
            return html;
        }

        try {
            // CSS 파일 읽기
            ClassPathResource cssResource = new ClassPathResource("static/news-clip.css");
            String cssContent = new String(cssResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // <link rel="stylesheet" href="/news-clip.css"> 태그를 찾아서 <style> 태그로 교체
            Pattern linkPattern = Pattern.compile("<link[^>]*rel=['\"]stylesheet['\"][^>]*href=['\"]/news-clip\\.css['\"][^>]*>", Pattern.CASE_INSENSITIVE);
            Matcher linkMatcher = linkPattern.matcher(html);

            if (linkMatcher.find()) {
                // CSS 링크 태그를 <style> 태그로 교체
                String styleTag = "<style>\n" + cssContent + "\n</style>";
                return linkMatcher.replaceAll(Matcher.quoteReplacement(styleTag));
            } else {
                // <head> 태그 내부에 <style> 태그 추가
                Pattern headPattern = Pattern.compile("(<head[^>]*>)", Pattern.CASE_INSENSITIVE);
                Matcher headMatcher = headPattern.matcher(html);
                if (headMatcher.find()) {
                    String styleTag = "<style>\n" + cssContent + "\n</style>";
                    return headMatcher.replaceFirst(headMatcher.group(1) + "\n    " + styleTag);
                }
            }

            return html;
        } catch (IOException e) {
            log.error("CSS 파일 읽기 실패", e);
            // CSS 파일을 읽지 못하면 원본 HTML 반환
            return html;
        }
    }

    /**
     * 커스텀 질문 기반 보고서 생성
     */
    public String generateCustomReport(String taskType, List<Map<String, Object>> tasks, String reportType, String question) {
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
        
        if (reportType.equals("monthly")) {
            promptBuilder.append(String.format("다음은 %s 과제들의 %d년 %s 활동 내역입니다.\n\n", taskType, year, monthName));
        } else {
            promptBuilder.append(String.format("다음은 %s 과제들의 지금까지의 모든 활동 내역입니다.\n\n", taskType));
        }
        
        for (Map<String, Object> task : tasks) {
            String taskName = (String) task.get("taskName");
            if (reportType.equals("monthly")) {
                String activityContent = (String) task.get("activityContent");
                if (activityContent != null && !activityContent.trim().isEmpty()) {
                    promptBuilder.append(String.format("- **%s**: %s\n\n", taskName, activityContent));
                }
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> activities = (List<Map<String, Object>>) task.get("activities");
                if (activities != null && !activities.isEmpty()) {
                    promptBuilder.append(String.format("### %s\n\n", taskName));
                    for (Map<String, Object> activity : activities) {
                        Integer activityYear = (Integer) activity.get("activityYear");
                        Integer activityMonth = (Integer) activity.get("activityMonth");
                        String activityContent = (String) activity.get("activityContent");
                        if (activityContent != null && !activityContent.trim().isEmpty()) {
                            promptBuilder.append(String.format("- **%d년 %d월**: %s\n\n", activityYear, activityMonth, activityContent));
                        }
                    }
                }
            }
        }
        
        promptBuilder.append("\n위 활동 내역을 바탕으로 다음 질문에 대한 답변을 제공해주세요:\n\n");
        promptBuilder.append(question != null && !question.trim().isEmpty() ? question : "위 활동 내역을 종합적으로 분석하여 주요 성과와 개선점을 요약해주세요.");
        promptBuilder.append("\n\n답변은 구체적이고 전문적으로 작성해주세요.");
        
        return callAX4(promptBuilder.toString());
    }

    /**
     * 종합 보고서 생성
     */
    public String generateComprehensiveReport(String taskType, List<Map<String, Object>> tasks) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append(String.format("다음은 %s 과제들의 지금까지의 모든 활동 내역입니다.\n\n", taskType));
        
        int taskCount = 0;
        
        for (Map<String, Object> task : tasks) {
            String taskName = (String) task.get("taskName");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> activities = (List<Map<String, Object>>) task.get("activities");
            
            taskCount++;
            promptBuilder.append(String.format("### %d. %s\n\n", taskCount, taskName));
            
            if (activities != null && !activities.isEmpty()) {
                for (Map<String, Object> activity : activities) {
                    Integer activityYear = (Integer) activity.get("activityYear");
                    Integer activityMonth = (Integer) activity.get("activityMonth");
                    String activityContent = (String) activity.get("activityContent");
                    
                    if (activityContent != null && !activityContent.trim().isEmpty()) {
                        promptBuilder.append(String.format("- **%d년 %d월**: %s\n\n", activityYear, activityMonth, activityContent));
                    }
                }
            } else {
                promptBuilder.append("- 활동내역 없음\n\n");
            }
        }
        
        promptBuilder.append("위 정보를 바탕으로 전문적이고 체계적인 종합 보고서를 마크다운 형식으로 작성해주세요.\n\n");
        promptBuilder.append("## 보고서 작성 요구사항\n\n");
        promptBuilder.append("1. **보고서 구조**: 다음 섹션을 포함하여 작성해주세요.\n");
        promptBuilder.append("   - ## 1. 개요 및 목적\n");
        promptBuilder.append("   - ## 2. 과제별 주요 활동 내역\n");
        promptBuilder.append("   - ## 3. 전체 성과 분석\n");
        promptBuilder.append("   - ## 4. 주요 성과 요약\n");
        promptBuilder.append("   - ## 5. 향후 계획 및 개선방안\n\n");
        
        promptBuilder.append("2. **작성 스타일**:\n");
        promptBuilder.append("   - 각 섹션은 명확한 제목(##)으로 구분\n");
        promptBuilder.append("   - 시간순으로 활동내역을 정리하여 서술\n");
        promptBuilder.append("   - 구체적이고 객관적인 서술\n");
        promptBuilder.append("   - 불필요한 인사말이나 서론 없이 핵심 내용만 작성\n");
        promptBuilder.append("   - 숫자나 통계가 있으면 포함\n\n");
        
        promptBuilder.append("3. **활동내역 처리**:\n");
        promptBuilder.append("   - 각 과제별로 시간순으로 활동내역을 상세히 서술\n");
        promptBuilder.append("   - 활동내역의 변화와 발전 과정을 포함\n");
        promptBuilder.append("   - 주요 성과와 결과를 강조\n\n");
        
        promptBuilder.append("4. **형식**:\n");
        promptBuilder.append("   - 마크다운 문법 사용 (##, **, -, 등)\n");
        promptBuilder.append("   - 가독성을 위한 적절한 줄바꿈\n");
        promptBuilder.append("   - 각 섹션은 충분한 내용으로 구성\n\n");
        
        promptBuilder.append("보고서를 작성해주세요.");
        
        return callAX4(promptBuilder.toString());
    }
}
